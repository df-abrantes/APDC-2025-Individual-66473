import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class ChangeStatePage extends StatefulWidget {
  const ChangeStatePage({super.key});

  @override
  State<ChangeStatePage> createState() => _ChangeStatePageState();
}

class _ChangeStatePageState extends State<ChangeStatePage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  String? _selectedState;
  bool _isLoading = false;

  final List<String> _states = ['ATIVADA', 'SUSPENSA', 'DESATIVADA'];

  Future<void> _submitChangeState() async {
    if (!_formKey.currentState!.validate() || _selectedState == null) return;

    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    if (token == null) {
      _showSnackBar('Token não encontrado.', Colors.red);
      return;
    }

    setState(() {
      _isLoading = true;
    });

    final response = await http.post(
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/changestate'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'token': token,
        'email': _emailController.text,
        'newState': _selectedState,
      }),
    );

    setState(() {
      _isLoading = false;
    });

    // Tratar diferentes tipos de erro dependendo da resposta do servidor
    if (response.statusCode == 200) {
      _showSnackBar('Estado alterado com sucesso!', Colors.green);
    } else {
      String errorMessage = 'Erro ao alterar o estado.';
      try {
        final errorResponse = jsonDecode(utf8.decode(response.bodyBytes));
        if (errorResponse is Map && errorResponse.containsKey('message')) {
          errorMessage = errorResponse['message'];
        }

        // Lógica para diferenciar mensagens específicas
        if (errorMessage.contains("Campos obrigatórios")) {
          errorMessage = 'Verifique se todos os campos foram preenchidos corretamente.';
        } else if (errorMessage.contains("Token inválido") || errorMessage.contains("expirado")) {
          errorMessage = 'Sessão expirada ou token inválido. Por favor, faça login novamente.';
        } else if (errorMessage.contains("não têm permissões")) {
          errorMessage = 'Você não tem permissões para alterar o estado desta conta.';
        } else if (errorMessage.contains("Conta alvo não encontrada")) {
          errorMessage = 'Conta não encontrada. Verifique o e-mail da conta.';
        } else if (errorMessage.contains("Permissão insuficiente")) {
          errorMessage = 'Você não tem permissão para mudar o estado da conta.';
        }
      } catch (e) {
        print('Erro ao tentar ler a mensagem do erro: $e');
      }
      _showSnackBar(errorMessage, Colors.red);
    }
  }

  void _showSnackBar(String message, Color backgroundColor) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: backgroundColor,
      ),
    );
  }

  Widget _buildEmailField() {
    return TextFormField(
      controller: _emailController,
      decoration: InputDecoration(
        labelText: '📧 Email da Conta',
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(color: Colors.black87),
      ),
      validator: (value) =>
          value == null || value.isEmpty ? 'Campo obrigatório' : null,
    );
  }

  Widget _buildStateDropdown() {
    return DropdownButtonFormField<String>(
      value: _selectedState,
      items: _states
          .map((state) => DropdownMenuItem(value: state, child: Text(state)))
          .toList(),
      onChanged: (value) => setState(() => _selectedState = value),
      decoration: InputDecoration(
        labelText: '🔄 Novo Estado',
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(color: Colors.black87),
      ),
      validator: (value) =>
          value == null ? 'Por favor, selecione um estado' : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F1D37),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.topLeft,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back, color: Colors.white),
                  onPressed: () => Navigator.pop(context),
                ),
              ),
              const Text(
                'Alterar Estado',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 16),
              Center(
                child: Container(
                  width: 500,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        _buildEmailField(),
                        const SizedBox(height: 12),
                        _buildStateDropdown(),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _submitChangeState,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF00B49F),
                              padding: const EdgeInsets.symmetric(vertical: 16),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            child: _isLoading
                                ? const CircularProgressIndicator(color: Colors.white)
                                : const Text(
                                    'Confirmar Alteração',
                                    style: TextStyle(
                                      fontSize: 16, color: Colors.white),
                                  ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
