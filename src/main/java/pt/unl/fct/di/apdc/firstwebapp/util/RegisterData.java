package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {

    public String username;
    public String email;
    public String password;
    public String nome_completo;
    public String telefone;
    public String perfil;

    // Campos opcionais
    public String numero_cartao_cidadao;
    public String nif_utilizador;
    public String entidade_empregadora;
    public String funcao;
    public String morada;
    public String nif_entidade_empregadora;

    public RegisterData() {}

    public RegisterData(String username, String email, String password, String nome_completo, String telefone, String perfil,
                        String numero_cartao_cidadao, String nif_utilizador, String entidade_empregadora,
                        String funcao, String morada, String nif_entidade_empregadora) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.nome_completo = nome_completo;
        this.telefone = telefone;
        this.perfil = perfil;
        this.numero_cartao_cidadao = numero_cartao_cidadao;
        this.nif_utilizador = nif_utilizador;
        this.entidade_empregadora = entidade_empregadora;
        this.funcao = funcao;
        this.morada = morada;
        this.nif_entidade_empregadora = nif_entidade_empregadora;
    }

    public boolean validRegistration() {
        return username != null && !username.trim().isEmpty()
            && email != null && !email.trim().isEmpty()
            && password != null && !password.trim().isEmpty()
            && nome_completo != null && !nome_completo.trim().isEmpty()
            && telefone != null && !telefone.trim().isEmpty()
            && perfil != null && !perfil.trim().isEmpty();
    }
}
