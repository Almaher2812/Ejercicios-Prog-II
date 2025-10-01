import java.util.*;
import java.time.*;

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

public class Ejemplo4Banco {
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

        // Flujo de prueba
        Map<String,String> H = new HashMap<>();
        System.out.println(gw.request("admin","1234","/banco/otp","",H));
        String otp = gw.otps.get("admin"); // simulamos "leer" el OTP
        H.put("X-OTP", otp);
        System.out.println(gw.request("admin","1234","/banco/transferir","Cuenta 123456",H));
    }
}
// EOF
