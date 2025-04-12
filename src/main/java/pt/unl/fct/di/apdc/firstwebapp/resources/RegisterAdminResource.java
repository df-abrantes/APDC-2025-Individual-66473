package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.mindrot.jbcrypt.BCrypt;

@Path("/registeradmin")
public class RegisterAdminResource {

    private static final Logger LOG = Logger.getLogger(RegisterAdminResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAdmin() {
        String username = "admin";
        String email = "admin@admin.pt";
        String password = "admin123";
        String nomeCompleto = "Administrador do Sistema";
        String telefone = "000000000";
        String perfil = "privado";
        String role = "ADMIN";
        String estadoConta = "ATIVADA";

        Key emailKey = datastore.newKeyFactory().setKind("User").newKey(email);
        Key usernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(username);

        Transaction txn = datastore.newTransaction();
        try {
            if (txn.get(emailKey) != null || txn.get(usernameKey) != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT)
                        .entity("{\"message\":\"Já existe uma conta de admin.\"}")
                        .build();
            }

            Entity userEntity = Entity.newBuilder(emailKey)
                    .set("username", username)
                    .set("nome_completo", nomeCompleto)
                    .set("telefone", telefone)
                    .set("password", BCrypt.hashpw(password, BCrypt.gensalt()))
                    .set("perfil", perfil)
                    .set("numero_cartao_cidadao", "NOT DEFINED")
                    .set("role", role)
                    .set("nif_utilizador", "NOT DEFINED")
                    .set("entidade_empregadora", "NOT DEFINED")
                    .set("funcao", "NOT DEFINED")
                    .set("morada", "NOT DEFINED")
                    .set("nif_entidade_empregadora", "NOT DEFINED")
                    .set("estado_conta", estadoConta)
                    .set("creation_time", Timestamp.now())
                    .build();

            Entity usernameEntity = Entity.newBuilder(usernameKey)
                    .set("email", email)
                    .build();

            txn.put(userEntity, usernameEntity);
            txn.commit();

            LOG.info("Conta ADMIN criada com sucesso.");
            return Response.status(Status.CREATED)
                    .entity("{\"message\":\"Conta ADMIN criada com sucesso.\"}")
                    .build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "Erro de base de dados: ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\":\"Erro ao criar conta admin: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
