package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeAttributesData;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

@Path("/changeattributes")
public class ChangeAttributesResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAttributes(String body) {
        ObjectMapper mapper = new ObjectMapper();
        ChangeAttributesData data;

        // Processamento do JSON de entrada
        try {
            data = mapper.readValue(body, ChangeAttributesData.class);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"JSON inválido.\"}")
                    .build();
        }

        // Verifica se o token está presente
        if (data.token == null || data.token.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Token em falta.\"}")
                    .build();
        }

        // Verifica se o email (chave primária na Kind User) está presente
        if (data.email == null || data.email.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Email do utilizador a atualizar é obrigatório.\"}")
                    .build();
        }

        try {
            Entity session = getSessionByToken(data.token);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"message\": \"Token inválido ou sessão inexistente.\"}")
                        .build();
            }
            String requesterEmail = session.getString("user_email");
            String requesterRole = session.getString("role");
            boolean isSelf = requesterEmail.equals(data.email);

            // Obter o usuário original (usando o email como chave primária)
            Entity user = getUserByEmail(data.email);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Utilizador não encontrado.\"}")
                        .build();
            }

            // Verifica validade da sessão (exemplo com campo validTo)
            if (user.contains("validTo")) {
                Timestamp validToTimestamp = user.getTimestamp("validTo");
                if (validToTimestamp != null) {
                    Instant validToInstant = validToTimestamp.toSqlTimestamp().toInstant();
                    if (validToInstant.isBefore(Instant.now())) {
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity("{\"message\": \"Sessão expirada. Por favor, volte a iniciar sessão.\"}")
                                .build();
                    }
                }
            }

            // Verifica permissões conforme o papel do utilizador
            if (requesterRole.equals("ENDUSER")) {
                if (!isSelf) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\": \"ENDUSER só pode alterar os próprios dados.\"}")
                            .build();
                }
                // ENDUSER não pode alterar username ou email
                if ((data.username != null && !data.username.isEmpty()) ||
                    (data.newEmail != null && !data.newEmail.isEmpty())) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\": \"ENDUSER não pode alterar username ou email.\"}")
                            .build();
                }
            } else if (requesterRole.equals("BACKOFFICE")) {
                // Regras para BACKOFFICE
                String estado = user.contains("estado") ? user.getString("estado") : "";
                if (!estado.equals("ATIVADA")) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\": \"BACKOFFICE só pode alterar utilizadores depois de ativar a conta.\"}")
                            .build();
                }
                if (!(user.getString("role").equals("ENDUSER") || user.getString("role").equals("PARTNER"))) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\": \"BACKOFFICE só pode alterar ENDUSER ou PARTNER.\"}")
                            .build();
                }
                // BACKOFFICE não pode alterar username ou email
                if ((data.username != null && !data.username.isEmpty()) ||
                    (data.newEmail != null && !data.newEmail.isEmpty())) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"message\": \"BACKOFFICE não pode alterar username ou email.\"}")
                            .build();
                }
            }

            Transaction txn = datastore.newTransaction();
            try {
                Entity updatedUser = user;
                // Se newEmail for fornecido, atualiza a entidade na Kind User (movendo-a para a nova chave primária)
                if (data.newEmail != null && !data.newEmail.isEmpty()) {
                    Key newUserKey = datastore.newKeyFactory().setKind("User").newKey(data.newEmail);
                    // Construir uma nova entidade User com base na original e o novo email (chave primária)
                    Entity.Builder userBuilder = Entity.newBuilder(newUserKey);
                    for (String property : user.getNames()) {
                        userBuilder.set(property, (Value<?>) user.getValue(property));
                    }
                    updatedUser = userBuilder.build();
                    // Remove o usuário antigo
                    txn.delete(user.getKey());
                }

                // Se o username for alterado, atualiza a entidade User e a Kind UserUsername.
                if (data.username != null && !data.username.isEmpty() &&
                    !data.username.equals(updatedUser.getString("username"))) {
                    updatedUser = Entity.newBuilder(updatedUser)
                            .set("username", data.username)
                            .build();

                    // Atualizar na Kind UserUsername:
                    // a) Remover a entrada antiga (chave antiga) – usamos o username antigo da entidade original
                    String oldUsername = user.getString("username");
                    Key oldUserUsernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(oldUsername);
                    txn.delete(oldUserUsernameKey);
                    
                    // b) Criar a nova associação: o novo username como chave,
                    //    e a propriedade "email" armazenando o email (chave primária) do User atualizado.
                    Key newUserUsernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(data.username);
                    Entity newUserUsername = Entity.newBuilder(newUserUsernameKey)
                            .set("email", updatedUser.getKey().getName())
                            .build();
                    txn.put(newUserUsername);
                } else if (data.newEmail != null && !data.newEmail.isEmpty()) {
                    // Se somente o email foi alterado, atualize a propriedade "email" na entidade UserUsername
                    String currentUsername = updatedUser.getString("username");
                    Key userUsernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(currentUsername);
                    Entity userUsername = datastore.get(userUsernameKey);
                    if (userUsername != null) {
                        userUsername = Entity.newBuilder(userUsername)
                                .set("email", updatedUser.getKey().getName())  // Novo email
                                .build();
                        txn.put(userUsername);
                    }
                }
                
                // Atualiza os demais campos (se enviados; caso contrário, mantém os originais)
                Map<String, Object> fields = new HashMap<>();
                fields.put("nome_completo", data.nome_completo);
                fields.put("perfil", data.perfil);
                fields.put("telefone", data.telefone);
                fields.put("nif_utilizador", data.nif_utilizador);
                fields.put("nif_entidade_empregadora", data.nif_entidade_empregadora);
                fields.put("morada", data.morada);
                fields.put("funcao", data.funcao);
                fields.put("entidade_empregadora", data.entidade_empregadora);
                fields.put("numero_cartao_cidadao", data.numero_cartao_cidadao);

                Entity.Builder builder = Entity.newBuilder(updatedUser);
                for (Map.Entry<String, Object> entry : fields.entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                        builder.set(entry.getKey(), StringValue.newBuilder(entry.getValue().toString()).build());
                    }
                }
                txn.put(builder.build());
                txn.commit();
                
                // Após o commit, excluir sessões ativas na Kind Sessao associadas ao email antigo,
                // mas só se o email foi alterado
                if (data.newEmail != null && !data.newEmail.isEmpty()) {
                    deleteSessions(data.email);
                }
                
                return Response.ok("{\"message\": \"Atributos atualizados com sucesso.\"}").build();
            } catch (Exception e) {
                if (txn.isActive()) txn.rollback();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"message\": \"Erro ao atualizar atributos: " + e.getMessage() + "\"}")
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Erro ao processar requisição: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // Método para buscar a sessão pelo token
    private Entity getSessionByToken(String token) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Sessao")
                .setFilter(PropertyFilter.eq("token", token))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    // Método para buscar o usuário pelo email (email é a chave primária na Kind User)
    private Entity getUserByEmail(String email) {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);
        return datastore.get(userKey);
    }
    
    // Método para excluir todas as sessões ativas na Kind Sessao associadas ao email informado
    private void deleteSessions(String email) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Sessao")
                .setFilter(PropertyFilter.eq("user_email", email))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        while (results.hasNext()) {
            Entity sessao = results.next();
            datastore.delete(sessao.getKey());
        }
    }
}
