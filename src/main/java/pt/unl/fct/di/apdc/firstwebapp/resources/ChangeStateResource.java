package pt.unl.fct.di.apdc.firstwebapp.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import com.google.cloud.datastore.*;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStateData;

@Path("/changestate")
public class ChangeStateResource {
    
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeState(ChangeStateData stateChangeRequest) {
        if (stateChangeRequest == null || !stateChangeRequest.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"message\": \"Campos obrigatórios em falta ou estado inválido.\"}")
                .build();
        }

        try {
            // Verifica o token e obtém o role do utilizador atual
            String userRole = getUserRoleByToken(stateChangeRequest.token);
            if (userRole == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Token inválido ou expirado.\"}")
                    .build();
            }

            // Verifica o role do utilizador que está a tentar mudar o estado da conta
            if (userRole.equals("PARTNER") || userRole.equals("ENDUSER")) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"message\": \"PARTNER e ENDUSER não têm permissões para mudar o estado da conta.\"}")
                    .build();
            }

            // Verifica a existência da conta alvo
            String currentState = getAccountState(stateChangeRequest.email);
            if (currentState == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\": \"Conta alvo não encontrada.\"}")
                    .build();
            }

            // Lógica de mudança de estado dependendo do role do utilizador
            boolean canChangeState = false;
            if (userRole.equals("ADMIN")) {
                canChangeState = true;
            } else if (userRole.equals("BACKOFFICE")) {
                if (!currentState.equals(stateChangeRequest.newState)) {
                    canChangeState = true;
                }
            }

            if (!canChangeState) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"message\": \"Permissão insuficiente para mudar o estado da conta.\"}")
                    .build();
            }

            // Executa a mudança de estado da conta
            boolean updated = updateAccountState(stateChangeRequest.email, stateChangeRequest.newState);
            if (updated) {
                return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Estado da conta alterado com sucesso.\"}")
                    .build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Erro ao alterar estado da conta.\"}")
                    .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"message\": \"Erro ao acessar a base de dados: " + e.getMessage() + "\"}")
                .build();
        }
    }

    private String getUserRoleByToken(String token) {
        // Lógica para obter o role do utilizador usando o token no Datastore
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Sessao")
            .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
            .build();

        QueryResults<Entity> results = datastore.run(query);
        Entity session = results.hasNext() ? results.next() : null;
        return session != null ? session.getString("role") : null;
    }

    private String getAccountState(String email) {
        // Lógica para verificar o estado da conta no Datastore usando o Kind correto
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);
        Entity user = datastore.get(userKey);
        return user != null ? user.getString("estado_conta") : null;
    }

    private boolean updateAccountState(String email, String newState) {
        // Lógica para atualizar o estado da conta no Datastore usando o Kind correto
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);
        Entity user = datastore.get(userKey);  // Obtém a entidade atual

        if (user != null) {
            // Cria uma nova entidade, preservando todos os campos existentes e atualizando o "estado_conta"
            Entity updatedUser = Entity.newBuilder(user)
                .set("estado_conta", newState)  // Atualiza o estado da conta
                .build();

            // Adiciona a entidade atualizada ao Datastore
            datastore.put(updatedUser);
            return true;
        }
        return false;  // Caso a entidade não seja encontrada
    }
}
