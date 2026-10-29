package pt.ipleiria.estg.dei.ei.dae.projeto.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResumoLimiterTest {

    @Test
    void nuncaExecutaMaisDoQueOLimiteEmSimultaneo() throws Exception {
        ResumoLimiter limiter = new ResumoLimiter(2, 10);
        AtomicInteger agora = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<String>> futuros = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futuros.add(pool.submit(() -> limiter.executar(() -> {
                int n = agora.incrementAndGet();
                maximo.accumulateAndGet(n, Math::max);
                Thread.sleep(40);
                agora.decrementAndGet();
                return "ok";
            }, 10_000)));
        }
        for (Future<String> f : futuros) {
            assertEquals("ok", f.get());
        }
        pool.shutdown();
        assertEquals(2, maximo.get());
        assertEquals(2, limiter.livres(), "todas as permissões devolvidas");
    }

    @Test
    void rejeitaQuandoAFilaEstaCheia() throws Exception {
        ResumoLimiter limiter = new ResumoLimiter(1, 1);
        CountDownLatch comecou = new CountDownLatch(1);
        CountDownLatch libertar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);

        pool.submit(() -> limiter.executar(() -> { comecou.countDown(); libertar.await(); return 1; }, 10_000));
        comecou.await();
        pool.submit(() -> limiter.executar(() -> 2, 10_000)); // fica à espera (fila: 1 de 1)
        while (limiter.aEsperar() < 1) {
            Thread.sleep(5);
        }
        assertThrows(ResumoLimiter.FilaCheiaException.class, () -> limiter.executar(() -> 3, 10_000));

        libertar.countDown();
        pool.shutdown();
    }

    @Test
    void desisteSeAVezNaoChegarATempo() throws Exception {
        ResumoLimiter limiter = new ResumoLimiter(1, 5);
        CountDownLatch comecou = new CountDownLatch(1);
        CountDownLatch libertar = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        pool.submit(() -> limiter.executar(() -> { comecou.countDown(); libertar.await(); return 1; }, 10_000));
        comecou.await();

        assertThrows(ResumoLimiter.EsperaExcedidaException.class, () -> limiter.executar(() -> 2, 50));
        libertar.countDown();
        pool.shutdown();
    }

    @Test
    void devolvePermissaoMesmoQuandoATarefaFalha() {
        ResumoLimiter limiter = new ResumoLimiter(1, 1);
        assertThrows(IllegalStateException.class, () -> limiter.executar(() -> { throw new IllegalStateException(); }, 1000));
        assertEquals(1, limiter.livres());
    }
}
