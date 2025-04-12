package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.cloud.Timestamp;
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
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {
    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public RegisterResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterData data) {
        LOG.fine("Tentativa de registo: " + data.username);

        if (!data.validRegistration()) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Todos os campos obrigatórios devem ser preenchidos!\"}")
                .build();
        }

        // Validando o formato do email
        if (!isValidEmail(data.email)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Email inválido.\"}")
                .build();
        }

        // Validando o formato do username (somente minúsculas)
        if (!isValidUsername(data.username)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Username inválido (deve ser apenas minúsculas).\"}")
                .build();
        }

        // Validando o telefone (somente números)
        if (!isValidPhone(data.telefone)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Telefone inválido (somente números).\"}")
                .build();
        }

        // Validando o cartão de cidadão (somente números)
        if (!isValidCC(data.numero_cartao_cidadao)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Número do Cartão de Cidadão inválido (somente números).\"}")
                .build();
        }

        // Validando o NIF (somente números)
        if (!isValidNIF(data.nif_utilizador)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"NIF do usuário inválido (somente números).\"}")
                .build();
        }

        // Validando o NIF da entidade empregadora
        if (!isValidNIF(data.nif_entidade_empregadora)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"NIF da entidade empregadora inválido (somente números).\"}")
                .build();
        }

        // Validando a senha (deve conter letras maiúsculas, minúsculas, números e sinais de pontuação)
        if (!isValidPassword(data.password)) {
            return Response.status(Status.BAD_REQUEST)
                .entity("{\"message\":\"Senha inválida (deve conter letras maiúscula e minúsculas, números e sinais de pontuação).\"}")
                .build();
        }

        String role = "ENDUSER";
        String estadoConta = "DESATIVADA";
        Key emailKey = datastore.newKeyFactory().setKind("User").newKey(data.email);
        Key usernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(data.username);

        Transaction txn = datastore.newTransaction();
        try {
            if (txn.get(emailKey) != null || txn.get(usernameKey) != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT)
                    .entity("{\"message\":\"Já existe uma conta com este email ou username.\"}")
                    .build();
            }

            Entity userEntity = Entity.newBuilder(emailKey)
                .set("username", data.username)
                .set("nome_completo", data.nome_completo)
                .set("telefone", data.telefone)
                .set("password", BCrypt.hashpw(data.password, BCrypt.gensalt()))
                .set("perfil", data.perfil)
                .set("numero_cartao_cidadao", data.numero_cartao_cidadao == null ? "NOT DEFINED" : data.numero_cartao_cidadao)
                .set("role", role)
                .set("nif_utilizador", data.nif_utilizador == null ? "NOT DEFINED" : data.nif_utilizador)
                .set("entidade_empregadora", data.entidade_empregadora == null ? "NOT DEFINED" : data.entidade_empregadora)
                .set("funcao", data.funcao == null ? "NOT DEFINED" : data.funcao)
                .set("morada", data.morada == null ? "NOT DEFINED" : data.morada)
                .set("nif_entidade_empregadora", data.nif_entidade_empregadora == null ? "NOT DEFINED" : data.nif_entidade_empregadora)
                .set("estado_conta", estadoConta)
                .set("creation_time", Timestamp.now())
                .build();

            Entity usernameEntity = Entity.newBuilder(usernameKey)
                .set("email", data.email)
                .build();

            txn.put(userEntity, usernameEntity);
            txn.commit();

            LOG.info("Conta criada com sucesso: " + data.username);
            return Response.status(Status.CREATED)
                .entity("{\"message\":\"Conta criada com sucesso\"}")
                .build();

        } catch (DatastoreException e) {
            if (txn.isActive()) txn.rollback();
            LOG.log(Level.SEVERE, "Erro de base de dados: ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("{\"message\":\"Erro ao criar conta: " + e.getMessage() + "\"}")
                .build();
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private boolean isValidUsername(String username) {
        return username != null && username.equals(username.toLowerCase());
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("[0-9]+");
    }

    private boolean isValidCC(String cc) {
        return cc != null && cc.matches("[0-9]+");
    }

    private boolean isValidNIF(String nif) {
        return nif != null && nif.matches("[0-9]+");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.matches(".*[a-z].*") && password.matches(".*[A-Z].*") &&
               password.matches(".*[0-9].*") && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
}
