package com.prdv.rdv.planning.adapter.out.notification;

import com.prdv.rdv.planning.application.port.output.PlanningNotificationGateway;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class SmtpPlanningNotificationGateway implements PlanningNotificationGateway {
    private final UserRepository users;
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    public SmtpPlanningNotificationGateway(UserRepository users,ObjectProvider<JavaMailSender> senders,
            @Value("${prdv.planning.notifications.from:no-reply@prdv.app}") String from) {
        this.users=users; this.senders=senders; this.from=from;
    }
    public void send(UUID eventId,Long recipient,String type) {
        JavaMailSender sender=senders.getIfAvailable();
        if(sender==null) throw new IllegalStateException("SMTP non configure");
        var user=users.findById(recipient).orElseThrow(() -> new IllegalStateException("Destinataire absent"));
        SimpleMailMessage mail=new SimpleMailMessage(); mail.setFrom(from); mail.setTo(user.getEmail());
        mail.setSubject("PRDV : une mise a jour vous attend");
        mail.setText("Une mise a jour de votre planning est disponible dans votre espace securise. Reference : "+eventId);
        sender.send(mail);
    }
}
