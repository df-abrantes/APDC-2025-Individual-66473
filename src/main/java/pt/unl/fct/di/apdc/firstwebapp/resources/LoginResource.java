package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.mindrot.jbcrypt.BCrypt;

import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
public class LoginResource {

    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    public static class LoginResponse {
        public String token;
        public String user;
        public String role;
        public Validity validity;

        public static class Validity {
            public String valid_from;
            public String valid_to;
            public String verificador;
        }
    }

    private Response jsonError(Status status, String message) {
        return Response.status(status)
                .entity("{\"message\":\"" + message + "\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginUser(LoginData data) {
        LOG.fine("Tentativa de login: " + data.username);

        if (data.username == null || data.password == null) {
            return jsonError(Status.BAD_REQUEST, "Username e password são obrigatórios.");
        }

        String userEmail;

        // Verificar se é username ou email
        if (data.username.contains("@")) {
            userEmail = data.username;
        } else {
            // Buscar email associado ao username
            Key usernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(data.username);
            Entity usernameEntity = datastore.get(usernameKey);
            if (usernameEntity == null) {
                return jsonError(Status.UNAUTHORIZED, "Utilizador não encontrado ou password errada.");
            }
            userEmail = usernameEntity.getString("email");
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(userEmail);
        Entity userEntity = datastore.get(userKey);
        if (userEntity == null || !BCrypt.checkpw(data.password, userEntity.getString("password"))) {
            return jsonError(Status.UNAUTHORIZED, "Utilizador não encontrado ou password errada.");
        }

        // Verificar se conta está suspensa
        if (userEntity.getString("estado_conta").equals("SUSPENSA")) {
            return jsonError(Status.FORBIDDEN, "Esta conta encontra-se suspensa.");
        }

        // Apagar sessões antigas
        Query<Entity> oldSessionsQuery = Query.newEntityQueryBuilder()
                .setKind("Sessao")
                .setFilter(StructuredQuery.PropertyFilter.eq("user_email", userEmail))
                .build();

        QueryResults<Entity> sessions = datastore.run(oldSessionsQuery);
        while (sessions.hasNext()) {
            datastore.delete(sessions.next().getKey());
        }

        // Criar nova sessão
        String token = UUID.randomUUID().toString();
        String verificador = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        long expires = now + (10 * 60 * 1000); // 10 minutos

        Key sessionKey = datastore.allocateId(datastore.newKeyFactory().setKind("Sessao").newKey());
        Entity session = Entity.newBuilder(sessionKey)
                .set("token", token)
                .set("user_email", userEmail)
                .set("role", userEntity.getString("role"))
                .set("valid_from", now)
                .set("valid_to", expires)
                .set("verificador", verificador)
                .build();

        datastore.put(session);

        // Criar resposta
        LoginResponse res = new LoginResponse();
        res.token = token;
        res.user = userEmail;
        res.role = userEntity.getString("role");
        LoginResponse.Validity v = new LoginResponse.Validity();
        v.valid_from = String.valueOf(now);
        v.valid_to = String.valueOf(expires);
        v.verificador = verificador;
        res.validity = v;

        return Response.ok(g.toJson(res), MediaType.APPLICATION_JSON).build();
    }
}
