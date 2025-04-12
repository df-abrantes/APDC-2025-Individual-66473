import 'package:flutter/material.dart';
import 'register_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Projeto',
      theme: ThemeData(primarySwatch: Colors.teal),
      home: const RegisterPage(), // <- Começa na página de registo
      debugShowCheckedModeBanner: false,
    );
  }
}
