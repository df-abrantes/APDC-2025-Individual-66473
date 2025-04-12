package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.util.ListUsersData;

@Path("/listusers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ListUsersResource {

    private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();

    @POST
    public Response listUsers(ListUsersData data) {
        LOG.info("Recebido pedido para listar utilizadores com token: " + data.token);

        if (data == null || data.token == null || data.token.isBlank()) {
            LOG.warning("Token em falta ou inválido.");
            return Response.status(Status.BAD_REQUEST)
                    .entity("{\"message\":\"Token em falta.\"}")
                    .build();
        }

        try {
            Query<Entity> sessionQuery = Query.newEntityQueryBuilder()
                    .setKind("Sessao")
                    .setFilter(StructuredQuery.PropertyFilter.eq("token", data.token))
                    .build();

            QueryResults<Entity> sessions = datastore.run(sessionQuery);

            if (!sessions.hasNext()) {
                LOG.warning("Sessão não encontrada para o token.");
                return Response.status(Status.UNAUTHORIZED)
                        .entity("{\"message\":\"Token inválido ou sessão expirada.\"}")
                        .build();
            }

            Entity session = sessions.next();
            String role = session.getString("role");
            LOG.info("Token associado ao role: " + role);

            EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder().setKind("User");

            switch (role) {
                case "ADMIN":
                    break;
                case "BACKOFFICE":
                    queryBuilder.setFilter(StructuredQuery.PropertyFilter.eq("role", "ENDUSER"));
                    break;
                case "ENDUSER":
                    queryBuilder.setFilter(StructuredQuery.CompositeFilter.and(
                            StructuredQuery.PropertyFilter.eq("role", "ENDUSER"),
                            StructuredQuery.PropertyFilter.eq("perfil", "publico"),
                            StructuredQuery.PropertyFilter.eq("estado_conta", "ATIVADA")
                    ));
                    break;
                default:
                    LOG.warning("Permissões insuficientes para o role: " + role);
                    return Response.status(Status.FORBIDDEN)
                            .entity("{\"message\":\"Permissões insuficientes.\"}")
                            .build();
            }

            QueryResults<Entity> results = datastore.run(queryBuilder.build());
            List<Object> users = new ArrayList<>();

            while (results.hasNext()) {
                Entity user = results.next();
                Map<String, String> userMap = new HashMap<>();
            
                // Aceder ao email a partir da chave da entidade.
                Key userKey = user.getKey();  // Obtém a chave da entidade
                String email = userKey.getName();  // A chave primária contém o email (ID)
                if (email != null && !email.isEmpty()) {
                    userMap.put("email", email);
                } else {
                    LOG.warning("Utilizador sem email encontrado.");
                }
            
                // Adicionar informações adicionais
                userMap.put("username", user.getString("username"));
                userMap.put("nome_completo", user.getString("nome_completo"));
            
                // Se o role não for ENDUSER, adicionar todos os campos
                if (!"ENDUSER".equals(role)) {
                    for (String field : user.getNames()) {
                        Value<?> val = user.getValue(field);
                        String value;
                        if (val instanceof StringValue) {
                            value = ((StringValue) val).get();
                        } else if (val instanceof TimestampValue) {
                            value = ((TimestampValue) val).get().toString();
                        } else {
                            value = val.get().toString();
                        }
                        userMap.put(field, value);
                    }
                }
            
                users.add(userMap);
            }
            

            LOG.info("Utilizadores listados com sucesso.");
            return Response.ok(g.toJson(users)).build();

        } catch (Exception e) {
            LOG.severe("Erro ao listar utilizadores: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Erro interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
