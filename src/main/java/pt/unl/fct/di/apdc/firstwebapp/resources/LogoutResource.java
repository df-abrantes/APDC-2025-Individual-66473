package pt.unl.fct.di.apdc.firstwebapp.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import com.google.cloud.datastore.*;

import pt.unl.fct.di.apdc.firstwebapp.util.LogoutData;

@Path("/logout")
public class LogoutResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(LogoutData data) {
        if (data.token == null || data.token.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Token em falta.\"}")
                    .build();
        }

        Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
        Transaction txn = datastore.newTransaction();

        try {
            // Procurar a entidade Sessao com o token fornecido
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Sessao")
                    .setFilter(StructuredQuery.PropertyFilter.eq("token", data.token))
                    .build();

            QueryResults<Entity> results = datastore.run(query);

            if (results.hasNext()) {
                Entity sessao = results.next();
                Key sessionKey = sessao.getKey();

                txn.delete(sessionKey);
                txn.commit();
                return Response.ok("{\"message\": \"Logout efetuado com sucesso.\"}").build();
            } else {
                return Response.ok("{\"message\": \"Token já inválido ou inexistente.\"}").build();
            }

        } catch (Exception e) {
            if (txn.isActive()) txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Erro ao processar logout: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
