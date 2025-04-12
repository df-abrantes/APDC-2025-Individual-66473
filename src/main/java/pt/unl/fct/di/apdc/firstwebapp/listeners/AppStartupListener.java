package pt.unl.fct.di.apdc.firstwebapp.listeners;

import java.util.logging.Logger;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.Timestamp;
import org.apache.commons.codec.digest.DigestUtils;
import org.mindrot.jbcrypt.BCrypt;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Initializing application...");
        createRootUser();
    }

    private void createRootUser() {
        String email = "root@user.com"; // Email usado como chave primária para a kind User
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(email);

        // Verifica se o usuário root já existe
        Entity user = datastore.get(userKey);
        if (user != null) {
            LOG.info("Root user already exists.");
            return;
        }

        // Dados do usuário root
        String username = "root";
        String nome_completo = "Root";
        String password = "root"; // Senha do root
        String telefone = "000000000";
        String perfil = "privado";
        String role = "ADMIN";
        String estado_conta = "ATIVADA";

        // Criação do Entity para o usuário root (kind User)
        Entity rootUser = Entity.newBuilder(userKey)
            .set("username", username)
            .set("nome_completo", nome_completo)
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
            .set("estado_conta", estado_conta)
            .set("creation_time", Timestamp.now())
            .build();

        // Adiciona o usuário root à kind User
        datastore.put(rootUser);
        LOG.info("Root user created in User kind successfully.");

        // Agora adiciona o root à kind UserUsername
        Key userUsernameKey = datastore.newKeyFactory().setKind("UserUsername").newKey(username);
        Entity rootUserUsername = Entity.newBuilder(userUsernameKey)
            .set("email", email)  // Associando o email ao username
            .build();

        // Adiciona o usuário root à kind UserUsername
        datastore.put(rootUserUsername);
        LOG.info("Root user created in UserUsername kind successfully.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application shutting down...");
    }
}
