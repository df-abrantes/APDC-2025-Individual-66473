import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

class RemoveAccountPage extends StatefulWidget {
  const RemoveAccountPage({super.key});

  @override
  State<RemoveAccountPage> createState() => _RemoveAccountPageState();
}

class _RemoveAccountPageState extends State<RemoveAccountPage> {
  final _formKey = GlobalKey<FormState>();
  final _identifierController = TextEditingController(); // Pode ser email ou username
  bool _isLoading = false;

  Future<void> _submitRemoveAccount() async {
    if (!_formKey.currentState!.validate()) return;

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
      Uri.parse('https://agile-stratum-456218-k3.oa.r.appspot.com/rest/removeaccount'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'token': token,
        'email': _identifierController.text, // ainda usamos o campo 'email' no JSON
      }),
    );

    setState(() {
      _isLoading = false;
    });

    if (response.statusCode == 200) {
      _showSnackBar('Conta removida com sucesso.', Colors.green);
    } else {
      String errorMessage = 'Erro ao remover conta.';
      try {
        final errorResponse = jsonDecode(utf8.decode(response.bodyBytes));
        if (errorResponse is Map && errorResponse.containsKey('message')) {
          errorMessage = errorResponse['message'];
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

  Widget _buildIdentifierField() {
    return TextFormField(
      controller: _identifierController,
      decoration: InputDecoration(
        labelText: '👤 Email ou Username da Conta a Remover',
        filled: true,
        fillColor: const Color(0xFFE5E7EB),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        labelStyle: const TextStyle(color: Colors.black87),
      ),
      validator: (value) {
        if (value == null || value.trim().isEmpty) return 'Campo obrigatório';
        return null;
      },
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
                'Remover Conta',
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
                        _buildIdentifierField(),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _submitRemoveAccount,
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
                                    'Confirmar Remoção',
                                    style: TextStyle(fontSize: 16, color: Colors.white),
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
