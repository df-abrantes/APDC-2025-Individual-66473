package pt.unl.fct.di.apdc.firstwebapp.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;
import com.google.cloud.datastore.*;

@Path("/profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProfileResource {

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    public Response getProfile(Map<String, String> data) {
        try {
            String token = data.get("token");

            if (token == null || token.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(jsonMessage("Token é obrigatório")).build();
            }

            // Verificar sessão
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Sessao")
                    .setFilter(StructuredQuery.PropertyFilter.eq("token", token))
                    .build();

            QueryResults<Entity> results = datastore.run(query);

            if (!results.hasNext()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(jsonMessage("Sessão inválida ou expirada")).build();
            }

            Entity session = results.next();
            String email = session.getString("user_email");

            // Obter dados do utilizador
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);
            Entity user = datastore.get(userKey);

            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(jsonMessage("Utilizador não encontrado")).build();
            }

            // Construir JSON com todos os atributos
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("username", getString(user, "username"));
            userData.put("nome_completo", getString(user, "nome_completo"));
            userData.put("numero_cartao_cidadao", getString(user, "numero_cartao_cidadao"));
            userData.put("nif_utilizador", getString(user, "nif_utilizador"));
            userData.put("telefone", getString(user, "telefone"));
            userData.put("morada", getString(user, "morada"));
            userData.put("funcao", getString(user, "funcao"));
            userData.put("entidade_empregadora", getString(user, "entidade_empregadora"));
            userData.put("nif_entidade_empregadora", getString(user, "nif_entidade_empregadora"));
            userData.put("estado_conta", getString(user, "estado_conta"));
            userData.put("perfil", getString(user, "perfil"));
            userData.put("role", getString(user, "role"));

            return Response.ok(userData).build();

        } catch (Exception e) {
            e.printStackTrace(); // debug
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(jsonMessage("Erro interno: " + e.getMessage())).build();
        }
    }

    private String getString(Entity user, String field) {
        return user.contains(field) ? user.getString(field) : "";
    }

    private Map<String, String> jsonMessage(String msg) {
        Map<String, String> res = new HashMap<>();
        res.put("error", msg);
        return res;
    }
}
