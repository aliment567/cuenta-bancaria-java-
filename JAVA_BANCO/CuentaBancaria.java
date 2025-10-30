import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CuentaBancaria {
    private static final AtomicInteger SEQ = new AtomicInteger(1);

    public enum TipoCuenta { CORRIENTE, AHORROS }

    private final int id;
    private final String cliente;
    private final TipoCuenta tipo;
    private double saldo;
    private final List<Transaccion> historial = new ArrayList<>();

    public CuentaBancaria(String cliente, TipoCuenta tipo, double saldoInicial) {
        this.id = SEQ.getAndIncrement();
        this.cliente = Objects.requireNonNull(cliente, "Cliente no puede ser null");
        this.tipo = Objects.requireNonNull(tipo, "Tipo de cuenta no puede ser null");
        this.saldo = Math.max(0.0, saldoInicial);
        registrarTransaccion(Transaccion.Tipo.DEPOSITO, saldoInicial); // inicial
    }

    public int getId() { return id; }
    public String getCliente() { return cliente; }
    public TipoCuenta getTipo() { return tipo; }

    public synchronized double getSaldo() { return saldo; }

    public synchronized void depositar(double cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad a depositar debe ser mayor que 0");
        saldo += cantidad;
        registrarTransaccion(Transaccion.Tipo.DEPOSITO, cantidad);
    }

    public synchronized void retirar(double cantidad) throws InsufficientFundsException {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad a retirar debe ser mayor que 0");
        if (cantidad > saldo) throw new InsufficientFundsException("Saldo insuficiente");
        saldo -= cantidad;
        registrarTransaccion(Transaccion.Tipo.RETIRO, cantidad);
    }

    public synchronized void aplicarInteres(double tasa) {
        if (tipo != TipoCuenta.AHORROS) return;
        double interes = saldo * tasa;
        saldo += interes;
        registrarTransaccion(Transaccion.Tipo.INTERES, interes);
    }

    public synchronized void aplicarCargoMensual(double monto) throws InsufficientFundsException {
        if (tipo != TipoCuenta.CORRIENTE) return;
        if (monto > saldo) throw new InsufficientFundsException("Saldo insuficiente para el cargo");
        saldo -= monto;
        registrarTransaccion(Transaccion.Tipo.CARGO, monto);
    }

    private void registrarTransaccion(Transaccion.Tipo tipo, double monto) {
        historial.add(new Transaccion(tipo, monto, saldo));
    }

    public List<Transaccion> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    @Override
    public String toString() {
        return String.format("ID:%d - %s (%s) - Saldo: %.2f", id, cliente, tipo, saldo);
    }

    // ---- CLASES INTERNAS ----
    public static class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String msg) { super(msg); }
    }

    public static class Transaccion {
        public enum Tipo { DEPOSITO, RETIRO, TRANSFERENCIA, INTERES, CARGO }
        private final Tipo tipo;
        private final double monto;
        private final LocalDateTime fecha;
        private final double saldoResultante;

        public Transaccion(Tipo tipo, double monto, double saldoResultante) {
            this.tipo = tipo;
            this.monto = monto;
            this.saldoResultante = saldoResultante;
            this.fecha = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %.2f -> Saldo: %.2f", fecha, tipo, monto, saldoResultante);
        }
    }

    // ---- BANCO ----
    static class Banco {
        private final Map<Integer, CuentaBancaria> cuentas = new LinkedHashMap<>();

        public CuentaBancaria crearCuenta(String cliente, TipoCuenta tipo, double saldoInicial) {
            CuentaBancaria c = new CuentaBancaria(cliente, tipo, saldoInicial);
            cuentas.put(c.getId(), c);
            return c;
        }

        public Optional<CuentaBancaria> obtenerCuenta(int id) {
            return Optional.ofNullable(cuentas.get(id));
        }

        public Collection<CuentaBancaria> listar() {
            return Collections.unmodifiableCollection(cuentas.values());
        }

        public void transferir(int fromId, int toId, double monto) throws InsufficientFundsException {
            if (monto <= 0) throw new IllegalArgumentException("Monto debe ser positivo");
            CuentaBancaria origen = cuentas.get(fromId);
            CuentaBancaria destino = cuentas.get(toId);
            if (origen == null || destino == null) throw new NoSuchElementException("Cuenta origen o destino no encontrada");

            synchronized (origen) {
                synchronized (destino) {
                    if (origen.getSaldo() < monto) throw new InsufficientFundsException("Saldo insuficiente");
                    origen.saldo -= monto;
                    destino.saldo += monto;
                    origen.registrarTransaccion(Transaccion.Tipo.TRANSFERENCIA, -monto);
                    destino.registrarTransaccion(Transaccion.Tipo.TRANSFERENCIA, monto);
                }
            }
        }

        public List<Transaccion> obtenerHistorial(int id) {
            CuentaBancaria c = cuentas.get(id);
            if (c == null) throw new NoSuchElementException("Cuenta no encontrada");
            return c.getHistorial();
        }

        public void aplicarInteresesYCargos(double tasaInteresAhorros, double cargoCorriente) {
            for (CuentaBancaria c : cuentas.values()) {
                try {
                    if (c.getTipo() == TipoCuenta.AHORROS) c.aplicarInteres(tasaInteresAhorros);
                    if (c.getTipo() == TipoCuenta.CORRIENTE) c.aplicarCargoMensual(cargoCorriente);
                } catch (InsufficientFundsException ignored) {}
            }
        }
    }

    // ---- CLI ----
    public static void main(String[] args) {
        Banco banco = new Banco();
        banco.crearCuenta("Tony Stark", TipoCuenta.CORRIENTE, 1599.99);

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n********************");
                System.out.println("1 - Crear cuenta");
                System.out.println("2 - Consultar saldo");
                System.out.println("3 - Retirar");
                System.out.println("4 - Depositar");
                System.out.println("5 - Listar cuentas");
                System.out.println("6 - Transferir entre cuentas");
                System.out.println("7 - Listar historial de cuenta");
                System.out.println("8 - Aplicar intereses/cargos globales");
                System.out.println("9 - Salir");
                System.out.print("Seleccione opción: ");

                String linea = sc.nextLine().trim();
                if (linea.isEmpty()) continue;

                int opcion;
                try { opcion = Integer.parseInt(linea); }
                catch (NumberFormatException e) { System.out.println("Opción inválida."); continue; }

                switch (opcion) {
                    case 1 -> crearCuentaFlow(sc, banco);
                    case 2 -> consultarSaldoFlow(sc, banco);
                    case 3 -> retirarFlow(sc, banco);
                    case 4 -> depositarFlow(sc, banco);
                    case 5 -> listarFlow(banco);
                    case 6 -> transferirFlow(sc, banco);
                    case 7 -> historialFlow(sc, banco);
                    case 8 -> aplicarInteresesFlow(sc, banco);
                    case 9 -> { System.out.println("Saliendo..."); return; }
                    default -> System.out.println("Opción no válida.");
                }
            }
        }
    }

    // ---- Flujos CLI ----
    private static void crearCuentaFlow(Scanner sc, Banco banco) {
        System.out.print("Nombre del titular: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) { System.out.println("Nombre no puede estar vacío."); return; }

        System.out.print("Tipo (1=Corriente, 2=Ahorros): ");
        String t = sc.nextLine().trim();
        TipoCuenta tipo = "2".equals(t) ? TipoCuenta.AHORROS : TipoCuenta.CORRIENTE;

        System.out.print("Saldo inicial: ");
        double saldoInicial;
        try { saldoInicial = Double.parseDouble(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Saldo inválido."); return; }

        CuentaBancaria c = banco.crearCuenta(nombre, tipo, saldoInicial);
        System.out.println("Cuenta creada: " + c);
    }

    private static void consultarSaldoFlow(Scanner sc, Banco banco) {
        obtenerCuentaPorId(sc, banco).ifPresentOrElse(
            c -> System.out.println("Saldo: " + c.getSaldo()),
            () -> System.out.println("Cuenta no encontrada.")
        );
    }

    private static void retirarFlow(Scanner sc, Banco banco) {
        obtenerCuentaPorId(sc, banco).ifPresentOrElse(c -> {
            System.out.print("Cantidad a retirar: ");
            try {
                double monto = Double.parseDouble(sc.nextLine().trim());
                c.retirar(monto);
                System.out.println("Retiro exitoso. Nuevo saldo: " + c.getSaldo());
            } catch (NumberFormatException e) {
                System.out.println("Monto inválido.");
            } catch (InsufficientFundsException | IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }, () -> System.out.println("Cuenta no encontrada."));
    }

    private static void depositarFlow(Scanner sc, Banco banco) {
        obtenerCuentaPorId(sc, banco).ifPresentOrElse(c -> {
            System.out.print("Cantidad a depositar: ");
            try {
                double monto = Double.parseDouble(sc.nextLine().trim());
                c.depositar(monto);
                System.out.println("Depósito exitoso. Nuevo saldo: " + c.getSaldo());
            } catch (IllegalArgumentException e) {
                System.out.println("Monto inválido: " + e.getMessage());
            }
        }, () -> System.out.println("Cuenta no encontrada."));
    }

    private static void listarFlow(Banco banco) {
        Collection<CuentaBancaria> cuentas = banco.listar();
        if (cuentas.isEmpty()) {
            System.out.println("No hay cuentas.");
            return;
        }
        cuentas.forEach(System.out::println);
    }

    private static void transferirFlow(Scanner sc, Banco banco) {
        try {
            System.out.print("ID cuenta origen: ");
            int fromId = Integer.parseInt(sc.nextLine().trim());
            System.out.print("ID cuenta destino: ");
            int toId = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Monto a transferir: ");
            double monto = Double.parseDouble(sc.nextLine().trim());
            banco.transferir(fromId, toId, monto);
            System.out.println("Transferencia exitosa.");
        } catch (NumberFormatException e) {
            System.out.println("ID o monto inválido.");
        } catch (InsufficientFundsException | NoSuchElementException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void historialFlow(Scanner sc, Banco banco) {
        try {
            System.out.print("ID de la cuenta: ");
            int id = Integer.parseInt(sc.nextLine().trim());
            List<Transaccion> historial = banco.obtenerHistorial(id);
            historial.forEach(System.out::println);
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
        } catch (NoSuchElementException e) {
            System.out.println("Cuenta no encontrada.");
        }
    }

    private static void aplicarInteresesFlow(Scanner sc, Banco banco) {
        try {
            System.out.print("Tasa interés AHORROS (ej: 0.05 = 5%): ");
            double tasa = Double.parseDouble(sc.nextLine().trim());
            System.out.print("Cargo mensual CORRIENTE: ");
            double cargo = Double.parseDouble(sc.nextLine().trim());
            banco.aplicarInteresesYCargos(tasa, cargo);
            System.out.println("Intereses y cargos aplicados correctamente.");
        } catch (NumberFormatException e) {
            System.out.println("Valor numérico inválido.");
        }
    }

    private static Optional<CuentaBancaria> obtenerCuentaPorId(Scanner sc, Banco banco) {
        System.out.print("Ingrese ID de cuenta: ");
        try {
            int id = Integer.parseInt(sc.nextLine().trim());
            return banco.obtenerCuenta(id);
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
            return Optional.empty();
        }
    }

    public static AtomicInteger getSeq() {
        return SEQ;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}

        
