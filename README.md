# Proyecto: Sistema de Gestión Bancaria en Java

Este proyecto implementa un sistema de cuentas bancarias con soporte para cuentas corrientes y de ahorro, permitiendo realizar operaciones básicas mediante un menú interactivo en consola (CLI).

## Características principales

-Creación de cuentas bancarias con número ID único.

-Depósitos y retiros con control de saldo insuficiente.

-Transferencias entre cuentas.

-Aplicación de intereses a cuentas de ahorro y cargos a cuentas corrientes.

-Registro histórico de todas las transacciones realizadas.

-Control de concurrencia mediante sincronización (synchronized).

## Aparecerá un menú como este:

Compila y ejecuta el programa desde tu terminal:

********************
1 - Crear cuenta
2 - Consultar saldo
3 - Retirar
4 - Depositar
5 - Listar cuentas
6 - Transferir entre cuentas
7 - Listar historial de cuenta
8 - Aplicar intereses/cargos globales
9 - Salir
Seleccione opción:

## Nuevas Funciones (Opciones 6, 7 y 8)

Estas son las tres funciones nuevas añadidas recientemente al sistema:

### Método relacionado: Banco.transferir(int fromId, int toId, double monto)

Pasos:

1 El usuario ingresa el ID de la cuenta origen.

2 Ingresa el ID de la cuenta destino.

3 Especifica el monto a transferir.

4 El sistema valida que ambas cuentas existan y que haya saldo suficiente.

5 Se actualizan los saldos y se registran las transacciones en ambas cuentas.

### Ejemplo de salida:

ID cuenta origen: 1
ID cuenta destino: 2
Monto a transferir: 500
Transferencia exitosa.

### Método relacionado: Banco.obtenerHistorial(int id)

Qué muestra:

Tipo de transacción (Depósito, Retiro, Transferencia, Interés o Cargo)

Monto de la operación

Fecha y hora

Saldo resultante después de la operación

### Ejemplo de salida:

ID de la cuenta: 1
[2025-10-29T12:34:56] DEPOSITO: 1000.00 -> Saldo: 1000.00
[2025-10-29T13:10:45] RETIRO: 200.00 -> Saldo: 800.00
[2025-10-29T14:20:00] TRANSFERENCIA: -300.00 -> Saldo: 500.00

### Aplicar intereses/cargos globales

Aplica automáticamente:

- Interés a todas las cuentas de ahorro.

- Cargo mensual a todas las cuentas corrientes.

📘 Método relacionado: Banco.aplicarInteresesYCargos(double tasaInteresAhorros, double cargoCorriente)

Flujo:

1 El usuario ingresa la tasa de interés (por ejemplo, 0.05 = 5%).

2 Ingresa el cargo mensual (por ejemplo, 20).

3 El sistema recorre todas las cuentas y aplica la acción correspondiente según el tipo de cuenta.

4 Cada operación queda registrada en el historial.

### Ejemplo de salida:

Tasa interés AHORROS (ej: 0.05 = 5%): 0.03
Cargo mensual CORRIENTE: 15
Intereses y cargos aplicados correctamente.
