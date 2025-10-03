import java.time.*;
import java.util.*;

/**
 * BANCO: 2FA simulado (OTP) + auditoría de transferencias.
 * Login: admin/1234, user/abcd
 *
 * Demuestra:
 * - Flujo en dos pasos: solicitar OTP y usarlo para transferir
 * - Registro de auditoría de transacciones
 */
interface Service4 { String handle(String data, String user, Map<String,String> headers); }

/** Login simple */
class LoginService4 {
    private final Map<String,String> users = Map.of("admin","1234","user","abcd");
    boolean login(String u,String p){ return users.containsKey(u)&&users.get(u).equals(p); }
}

/** Gateway con generación y verificación de OTP */
class ApiGateway4 {
    private final LoginService4 login;
    private final Map<String,Service4> routes = new HashMap<>();
    final Map<String,String> otps = new HashMap<>(); // OTP por usuario
    final List<String> audit = new ArrayList<>();
    private final Random rnd = new Random();

    ApiGateway4(LoginService4 l){ this.login=l; }
    void register(String p, Service4 s){ routes.put(p,s); }

    String request(String u,String p,String path,String data, Map<String,String> headers){
        if(!login.login(u,p)) return "❌ Acceso denegado.";
        Service4 s = routes.get(path);
        if(s==null) return "❌ Ruta no encontrada.";
        return s.handle(data,u,headers);
    }

    /** Genera un OTP de 6 dígitos y lo asocia al usuario */
    String genOtp(String user){
        String otp = String.valueOf(100000 + rnd.nextInt(900000));
        otps.put(user,otp);
        return otp;
    }
}

public class Ejemplo4 {
    public static void main(String[] args){
        LoginService4 ls = new LoginService4();
        ApiGateway4 gw = new ApiGateway4(ls);

        // Paso 1: pedir OTP
        gw.register("/banco/otp",(data,user,headers)->"🔐 OTP generado: " + gw.genOtp(user));

        // Paso 2: transferir (requiere header X-OTP)
        gw.register("/banco/transferir",(data,user,headers)->{
            String provided = headers.getOrDefault("X-OTP","");
            if(!Objects.equals(gw.otps.get(user), provided)) return "❌ OTP inválido.";
            gw.audit.add("TX from="+user+" -> "+data+" at "+ Instant.now());
            return "💸 Transferencia OK a: " + data;
        });

        // Flujo de prueba: 10 transferencias
        Map<String,String> H = new HashMap<>();
        String usuario = "admin";
        String password = "1234";

        for(int i = 1; i <= 10; i++){
            // Generar OTP (simula pedir OTP)
            String otpResponse = gw.request(usuario, password, "/banco/otp", "", H);
            System.out.println("Iteración " + i + " - " + otpResponse);

            // "Leer" el OTP (simulación de recibirlo por SMS/email)
            String otp = gw.otps.get(usuario);
            // Preparar header con OTP
            H.put("X-OTP", otp);

            // Cuenta destino distinta por iteración
            String cuentaDestino = "Cuenta 123456-" + String.format("%02d", i);

            // Realizar transferencia
            String transferResponse = gw.request(usuario, password, "/banco/transferir", cuentaDestino, H);
            System.out.println(transferResponse);

            // Limpiar header para la próxima iteración (opcional)
            H.remove("X-OTP");
        }

        // Mostrar auditoría al final
        System.out.println("\n--- Auditoría de transacciones ---");
        for(String entry : gw.audit){
            System.out.println(entry);
        }
    }
}
// EOF
