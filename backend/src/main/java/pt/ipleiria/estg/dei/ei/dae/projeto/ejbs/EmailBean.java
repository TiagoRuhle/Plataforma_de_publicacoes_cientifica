package pt.ipleiria.estg.dei.ei.dae.projeto.ejbs;

import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import pt.ipleiria.estg.dei.ei.dae.projeto.config.AppConfig;

import java.util.Date;
import java.util.Properties;

/**
 * Envio de e-mail por SMTP simples (o contentor "smtp" do compose em desenvolvimento).
 * A sessão é construída a partir de variáveis de ambiente, sem depender de JNDI do WildFly.
 */
@Stateless
public class EmailBean {

    private static volatile Session sessao;

    private static Session sessao() {
        Session s = sessao;
        if (s == null) {
            synchronized (EmailBean.class) {
                if (sessao == null) {
                    Properties p = new Properties();
                    p.put("mail.smtp.host", AppConfig.smtpHost());
                    p.put("mail.smtp.port", AppConfig.smtpPort());
                    p.put("mail.smtp.connectiontimeout", "5000");
                    p.put("mail.smtp.timeout", "10000");
                    sessao = Session.getInstance(p);
                }
                s = sessao;
            }
        }
        return s;
    }

    public void enviar(String para, String assunto, String texto) throws MessagingException {
        MimeMessage mensagem = new MimeMessage(sessao());
        mensagem.setFrom(new InternetAddress(AppConfig.mailFrom()));
        mensagem.setRecipients(Message.RecipientType.TO, InternetAddress.parse(para, false));
        mensagem.setSubject(assunto, "UTF-8");
        mensagem.setText(texto, "UTF-8");
        mensagem.setSentDate(new Date());
        Transport.send(mensagem);
    }
}
