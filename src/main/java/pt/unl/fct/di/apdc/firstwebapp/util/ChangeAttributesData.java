package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeAttributesData {
    public String token;
    public String email; // email atual da conta a atualizar
    public String newEmail; // novo email (opcional, só ADMIN pode usar)
    public String nome_completo;
    public String perfil;
    public String telefone;
    public String username;
    public String nif_utilizador;
    public String nif_entidade_empregadora;
    public String morada;
    public String funcao;
    public String entidade_empregadora;
    public String numero_cartao_cidadao;
    
    // Estes são proibidos de serem alterados por este recurso
    public String estado;
    public String role;

    // Novo campo creation_time, representado como String (ou DateTime se necessário)
    public String creation_time; // Data de criação do utilizador, representada como String (ISO 8601)
    
    public ChangeAttributesData() {}
}
