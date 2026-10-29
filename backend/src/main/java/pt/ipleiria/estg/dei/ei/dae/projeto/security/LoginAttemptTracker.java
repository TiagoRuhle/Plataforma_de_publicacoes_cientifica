package pt.ipleiria.estg.dei.ei.dae.projeto.security;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Bloqueio temporário do login após demasiadas falhas seguidas (defesa contra força bruta).
 * Em memória e por instância do servidor. Nota: um atacante pode bloquear temporariamente uma
 * conta alvo; é o compromisso habitual sem CAPTCHA/IP rate-limit.
 */
public class LoginAttemptTracker {

    private static final class Estado {
        int falhas;
        long bloqueadoAte;
    }

    private final int maxFalhas;
    private final long bloqueioMs;
    private final LongSupplier relogio;
    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();

    public LoginAttemptTracker(int maxFalhas, Duration bloqueio) {
        this(maxFalhas, bloqueio, System::currentTimeMillis);
    }

    public LoginAttemptTracker(int maxFalhas, Duration bloqueio, LongSupplier relogioMs) {
        this.maxFalhas = maxFalhas;
        this.bloqueioMs = bloqueio.toMillis();
        this.relogio = relogioMs;
    }

    public boolean bloqueado(String username) {
        return segundosRestantes(username) > 0;
    }

    public long segundosRestantes(String username) {
        Estado e = estados.get(chave(username));
        if (e == null) {
            return 0;
        }
        synchronized (e) {
            long restante = e.bloqueadoAte - relogio.getAsLong();
            return restante > 0 ? (restante + 999) / 1000 : 0;
        }
    }

    public void registarFalha(String username) {
        Estado e = estados.computeIfAbsent(chave(username), k -> new Estado());
        synchronized (e) {
            long agora = relogio.getAsLong();
            if (e.bloqueadoAte != 0 && e.bloqueadoAte <= agora) {
                e.falhas = 0; // o bloqueio anterior expirou
                e.bloqueadoAte = 0;
            }
            e.falhas++;
            if (e.falhas >= maxFalhas) {
                e.bloqueadoAte = agora + bloqueioMs;
            }
        }
    }

    public void limpar(String username) {
        estados.remove(chave(username));
    }

    private static String chave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
