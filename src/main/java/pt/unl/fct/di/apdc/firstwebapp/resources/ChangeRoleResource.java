package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import java.io.IOException;
import java.time.Instant;

@Path("/changerole")
public class ChangeRoleResource {
    
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(String body) {
        ObjectMapper mapper = new ObjectMapper();
        ChangeRoleData changeRoleData; // Usando ChangeRoleData
        
        try {
            // Deserializando o corpo JSON para ChangeRoleData
            changeRoleData = mapper.readValue(body, ChangeRoleData.class);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"message\": \"JSON inválido.\"}")
                .build();
        }

        // Verificando se os dados estão válidos
        if (!changeRoleData.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"message\": \"Campos obrigatórios em falta.\"}")
                .build();
        }

        String token = changeRoleData.token;

        try {
            // Verificar token e obter role do utilizador atual
            Entity session = getSessionByToken(token);
            if (session == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Token inválido. Autentique-se novamente.\"}")
                    .build();
            }

            // Verifica a expiração da sessão
            Object validToObj = session.contains("valid_to") ? session.getValue("valid_to").get() : null;
            if (validToObj instanceof Long) {
                com.google.cloud.Timestamp validTo = com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                    (Long) validToObj / 1000, (int) ((Long) validToObj % 1000) * 1000000);
                if (validTo.toSqlTimestamp().toInstant().isBefore(Instant.now())) {
                    return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"message\": \"Sessão expirada. Por favor, volte a iniciar sessão.\"}")
                        .build();
                }
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Erro: Campo 'valid_to' inválido no banco de dados.\"}")
                    .build();
            }

            String userRole = session.getString("role");

            // Verifica se os campos obrigatórios estão presentes
            if (changeRoleData.email == null || changeRoleData.newRole == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Campos obrigatórios em falta.\"}")
                    .build();
            }

            // Obter o role atual da conta a ser modificada
            Entity target = getAccountByEmail(changeRoleData.email);
            if (target == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\": \"Conta alvo não encontrada.\"}")
                    .build();
            }

            String currentRole = target.getString("role");

            // Regras de permissão
            if (userRole.equals("ENDUSER") || userRole.equals("PARTNER")) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"message\": \"" + userRole + " não tem permissões para alterar roles.\"}")
                    .build();
            } else if (userRole.equals("BACKOFFICE")) {
                if (!(currentRole.equals("ENDUSER") && changeRoleData.newRole.equals("PARTNER"))
                    && !(currentRole.equals("PARTNER") && changeRoleData.newRole.equals("ENDUSER"))) {
                    return Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"message\": \"BACKOFFICE só pode alterar entre ENDUSER e PARTNER.\"}")
                        .build();
                }
            }
            // ADMIN pode fazer qualquer alteração

            // Preservar todos os outros campos e atualizar apenas o campo "role"
            Entity updated = Entity.newBuilder(target)
                .set("role", changeRoleData.newRole)  // Atualiza o role
                .build();

            datastore.put(updated);
            return Response.ok("{\"message\": \"Role atualizado com sucesso.\"}").build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"message\": \"Erro ao atualizar role: " + e.getMessage() + "\"}")
                .build();
        }
    }

    // Método para obter sessão por token
    private Entity getSessionByToken(String token) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Sessao")
            .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
            .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    // Método para obter conta por email
    private Entity getAccountByEmail(String email) {
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);
        return datastore.get(userKey);
    }
}
