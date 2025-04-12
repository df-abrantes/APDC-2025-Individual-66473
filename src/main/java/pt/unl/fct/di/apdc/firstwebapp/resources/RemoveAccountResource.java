package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import pt.unl.fct.di.apdc.firstwebapp.util.RemoveUserData;

import java.io.IOException;
import java.util.regex.Pattern;

@Path("/removeaccount")
public class RemoveAccountResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAccount(String body) {
        ObjectMapper mapper = new ObjectMapper();
        RemoveUserData data;

        try {
            data = mapper.readValue(body, RemoveUserData.class);
        } catch (IOException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"JSON inválido.\"}")
                    .build();
        }

        if (data.token == null || data.email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Campos obrigatórios em falta.\"}")
                    .build();
        }

        Entity sessionEntity = getSessionByToken(data.token);
        if (sessionEntity == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Token inválido ou sessão expirada.\"}")
                    .build();
        }

        String userRole = sessionEntity.getString("role");

        // Determinar se é email ou username
        String targetEmail = isEmail(data.email) ? data.email : getEmailFromUsername(data.email);
        if (targetEmail == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\": \"Conta alvo não encontrada (email ou username inválido).\"}")
                    .build();
        }

        Entity targetAccount = getAccountByEmail(targetEmail);
        if (targetAccount == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"message\": \"Conta alvo não encontrada.\"}")
                    .build();
        }

        String targetRole = targetAccount.getString("role");

        if (userRole.equals("ENDUSER") || userRole.equals("PARTNER")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"message\": \"Não tem permissões para remover contas.\"}")
                    .build();
        }

        if (userRole.equals("BACKOFFICE") &&
                !(targetRole.equals("ENDUSER") || targetRole.equals("PARTNER"))) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"message\": \"BACKOFFICE só pode remover contas ENDUSER ou PARTNER.\"}")
                    .build();
        }

        try {
            // Apagar sessões
            Query<Entity> sessionQuery = Query.newEntityQueryBuilder()
                    .setKind("Sessao")
                    .setFilter(StructuredQuery.PropertyFilter.eq("user_email", targetEmail))
                    .build();
            QueryResults<Entity> sessions = datastore.run(sessionQuery);
            while (sessions.hasNext()) {
                datastore.delete(sessions.next().getKey());
            }

            // Apagar a conta na Kind User
            Key accountKey = datastore.newKeyFactory().setKind("User").newKey(targetEmail);
            datastore.delete(accountKey);

            // Apagar da Kind UserUsername
            Key usernameKey = getUsernameKeyFromEmail(targetEmail);
            if (usernameKey != null) {
                datastore.delete(usernameKey);
            }

            return Response.ok("{\"message\": \"Conta removida com sucesso.\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Erro ao remover conta: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    private Entity getSessionByToken(String token) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Sessao")
                .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                .build();
        QueryResults<Entity> results = datastore.run(query);
        return results.hasNext() ? results.next() : null;
    }

    private Entity getAccountByEmail(String email) {
        Key key = datastore.newKeyFactory().setKind("User").newKey(email);
        return datastore.get(key);
    }

    private boolean isEmail(String value) {
        return Pattern.compile("^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$").matcher(value).matches();
    }

    private String getEmailFromUsername(String username) {
        Key key = datastore.newKeyFactory().setKind("UserUsername").newKey(username);
        Entity entity = datastore.get(key);
        if (entity != null && entity.contains("email")) {
            return entity.getString("email");
        }
        return null;
    }

    private Key getUsernameKeyFromEmail(String email) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("UserUsername")
                .setFilter(StructuredQuery.PropertyFilter.eq("email", email))
                .build();

        QueryResults<Entity> results = datastore.run(query);
        if (results.hasNext()) {
            return results.next().getKey();
        }
        return null;
    }
}
