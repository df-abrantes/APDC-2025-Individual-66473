package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.datastore.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangePasswordData;

import java.io.IOException;

@Path("/changepassword")
public class ChangePasswordResource {

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(String jsonBody) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ChangePasswordData data = mapper.readValue(jsonBody, ChangePasswordData.class);

        // 1. Validar campos obrigatórios
        if (data.token == null || data.currentPassword == null ||
            data.newPassword == null || data.confirmPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Campos obrigatórios em falta.\"}")
                    .build();
        }

        // 2. Validar que as novas passwords coincidem
        if (!data.newPassword.equals(data.confirmPassword)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"As novas passwords não coincidem.\"}")
                    .build();
        }

        // 3. Validar token
        Query<Entity> sessionQuery = Query.newEntityQueryBuilder()
                .setKind("Sessao")
                .setFilter(StructuredQuery.PropertyFilter.eq("token", StringValue.of(data.token)))
                .build();

        QueryResults<Entity> sessionResults = datastore.run(sessionQuery);
        if (!sessionResults.hasNext()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Token inválido ou expirado.\"}")
                    .build();
        }

        Entity sessionEntity = sessionResults.next();
        String userEmail = sessionEntity.getString("user_email");

        // 4. Obter utilizador
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(userEmail);
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null || !userEntity.contains("password")) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Erro ao obter a password do utilizador.\"}")
                    .build();
        }

        String storedHash = userEntity.getString("password");

        // 5. Verificar password atual
        if (!BCrypt.checkpw(data.currentPassword, storedHash)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"A password atual está incorreta.\"}")
                    .build();
        }

        // 6. Atualizar password
        String newHashedPassword = BCrypt.hashpw(data.newPassword, BCrypt.gensalt());
        Entity updatedUser = Entity.newBuilder(userEntity)
                .set("password", StringValue.of(newHashedPassword))
                .build();
        datastore.put(updatedUser);

        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Password alterada com sucesso.\"}")
                .build();
    }
}
