import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'login_page.dart';

class RegisterPage extends StatefulWidget {
  const RegisterPage({Key? key}) : super(key: key);

  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final _formKey = GlobalKey<FormState>();

  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController = TextEditingController();
  final TextEditingController _citizenCardController = TextEditingController();
  final TextEditingController _nifController = TextEditingController();
  final TextEditingController _employerController = TextEditingController();
  final TextEditingController _jobController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  final TextEditingController _employerNifController = TextEditingController();

  final List<String> _possibleProfiles = ['público', 'privado'];
  String _profile = 'público';

  @override
  void dispose() {
    _emailController.dispose();
    _usernameController.dispose();
    _nameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _citizenCardController.dispose();
    _nifController.dispose();
    _employerController.dispose();
    _jobController.dispose();
    _addressController.dispose();
    _employerNifController.dispose();
    super.dispose();
  }

  Future<void> _signUp() async {
    if (!_formKey.currentState!.validate()) return;

    if (_passwordController.text != _confirmPasswordController.text) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('As passwords não coincidem.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    final body = json.encode({
      'email': _emailController.text.trim(),
      'username': _usernameController.text.trim(),
      'nome_completo': _nameController.text.trim(),
      'telefone': _phoneController.text.trim(),
      'password': _passwordController.text,
      'perfil': _profile,
      'numero_cartao_cidadao': _citizenCardController.text.trim().isNotEmpty
          ? _citizenCardController.text.trim()
          : null,
      'nif_utilizador': _nifController.text.trim().isNotEmpty
          ? _nifController.text.trim()
          : null,
      'entidade_empregadora': _employerController.text.trim().isNotEmpty
          ? _employerController.text.trim()
          : null,
      'funcao': _jobController.text.trim().isNotEmpty
          ? _jobController.text.trim()
          : null,
      'morada': _addressController.text.trim().isNotEmpty
          ? _addressController.text.trim()
          : null,
      'nif_entidade_empregadora': _employerNifController.text.trim().isNotEmpty
          ? _employerNifController.text.trim()
          : null,
    });

    try {
      final response = await http.post(
        Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/register'),
        headers: {'Content-Type': 'application/json; charset=UTF-8'},
        body: body,
      );

      final responseData = jsonDecode(utf8.decode(response.bodyBytes));
      final message = responseData['message'] ?? 'Erro desconhecido';

      Color bgColor;
      if (response.statusCode == 201) {
        bgColor = Colors.green;
      } else if (response.statusCode == 409) {
        bgColor = Colors.orange;
      } else if (response.statusCode == 400) {
        bgColor = Colors.amber;
      } else {
        bgColor = Colors.red;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message), backgroundColor: bgColor),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Erro de rede ao tentar criar conta.'),
          backgroundColor: Colors.red,
        ),
      );
      debugPrint("Erro de rede: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Container(
            constraints: const BoxConstraints(maxWidth: 850),
            padding: const EdgeInsets.all(32.0),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              boxShadow: const [
                BoxShadow(
                  color: Colors.black26,
                  blurRadius: 12,
                  offset: Offset(0, 6),
                ),
              ],
            ),
            child: Form(
              key: _formKey,
              child: Column(
                children: [
                  const Text(
                    'Criar Conta',
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF0F1D37),
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 24),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Coluna da esquerda
                      Expanded(
                        child: Column(
                          children: [
                            _buildTextField(_emailController, 'Email', Icons.email_outlined,
                                validatorMsg: 'Email é obrigatório'),
                            const SizedBox(height: 16),
                            _buildTextField(_usernameController, 'Username', Icons.person_outline,
                                validatorMsg: 'Username é obrigatório'),
                            const SizedBox(height: 16),
                            _buildTextField(_nameController, 'Nome Completo', Icons.badge_outlined,
                                validatorMsg: 'Nome é obrigatório'),
                            const SizedBox(height: 16),
                            _buildTextField(_phoneController, 'Telefone', Icons.phone_outlined,
                                keyboardType: TextInputType.phone, validatorMsg: 'Telefone é obrigatório'),
                            const SizedBox(height: 16),
                            _buildTextField(_passwordController, 'Password', Icons.lock_outline,
                                obscureText: true, validatorMsg: 'Password é obrigatória'),
                            const SizedBox(height: 16),
                            _buildTextField(
                              _confirmPasswordController,
                              'Confirmar Password',
                              Icons.lock_outline,
                              obscureText: true,
                              validatorMsg: 'Confirmação obrigatória',
                            ),
                            const SizedBox(height: 16),
                            _buildDropdown('Perfil', _profile, _possibleProfiles,
                                (val) => setState(() => _profile = val!)),
                            const SizedBox(height: 16),
                          ],
                        ),
                      ),
                      const SizedBox(width: 24),
                      // Coluna da direita
                      Expanded(
                        child: Column(
                          children: [
                            _buildTextField(_citizenCardController, 'Cartão de Cidadão', Icons.credit_card),
                            const SizedBox(height: 16),
                            _buildTextField(_nifController, 'NIF Utilizador', Icons.numbers_outlined,
                                keyboardType: TextInputType.number),
                            const SizedBox(height: 16),
                            _buildTextField(_jobController, 'Função', Icons.work_outline),
                            const SizedBox(height: 16),
                            _buildTextField(_addressController, 'Morada', Icons.home_outlined),
                            const SizedBox(height: 16),
                            _buildTextField(_employerController, 'Entidade Empregadora', Icons.business_outlined),
                            const SizedBox(height: 16),
                            _buildTextField(_employerNifController, 'NIF da Entidade Empregadora',
                                Icons.business_outlined,
                                keyboardType: TextInputType.number),
                            const SizedBox(height: 24),
                            Align(
                              alignment: Alignment.bottomCenter,
                              child: Column(
                                children: [
                                  SizedBox(
                                    width: double.infinity,
                                    height: 56, // altura aumentada
                                    child: ElevatedButton(
                                      onPressed: _signUp,
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: const Color(0xFF00B49F),
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(12),
                                        ),
                                      ),
                                      child: const Text(
                                        'Sign Up',
                                        style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.bold,
                                          color: Colors.white,
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 16),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  TextButton(
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const LoginPage()),
                      );
                    },
                    child: const Text(
                      'Já tens conta? Inicia sessão',
                      style: TextStyle(color: Colors.grey),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTextField(
    TextEditingController controller,
    String label,
    IconData icon, {
    bool obscureText = false,
    TextInputType keyboardType = TextInputType.text,
    String? validatorMsg,
  }) {
    return TextFormField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      validator: validatorMsg != null
          ? (value) => value == null || value.trim().isEmpty ? validatorMsg : null
          : null,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        filled: true,
        fillColor: Colors.grey[200],
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
    );
  }

  Widget _buildDropdown(
    String label,
    String value,
    List<String> items,
    ValueChanged<String?> onChanged,
  ) {
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        filled: true,
        fillColor: Colors.grey[200],
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: value,
          items: items.map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
          onChanged: onChanged,
        ),
      ),
    );
  }
}
