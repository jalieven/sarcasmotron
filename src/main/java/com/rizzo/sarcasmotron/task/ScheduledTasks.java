package com.rizzo.sarcasmotron.task;

import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.domain.calc.VoteStats;
import com.rizzo.sarcasmotron.domain.mongodb.User;
import com.rizzo.sarcasmotron.domain.web.Stats;
import com.rizzo.sarcasmotron.domain.web.StatsRequest;
import com.rizzo.sarcasmotron.mongodb.MongoDBStatsRepository;
import com.rizzo.sarcasmotron.mongodb.MongoDBUserRepository;
import org.elasticsearch.common.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

public class ScheduledTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTasks.class);

    private String winnerPeriod;

    @Autowired
    private VoteCalculator voteCalculator;

    @Autowired
    private MongoDBStatsRepository mongoDBStatsRepository;

    @Autowired
    private MongoDBUserRepository mongoDBUserRepository;

    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.winnerCalculation.fromAddress}")
    private String fromEmailAddress;

    @Value("${mail.winnerCalculation.subject}")
    private String subject;

    public ScheduledTasks(String winnerPeriod) {
        this.winnerPeriod = winnerPeriod;
    }

    public void calculateWinner() throws MessagingException {
        LOGGER.info("calculateWinner");
        final List<String> users = voteCalculator.getDistinctSarcasticUsers();
        Stats stats = new Stats();
        final ReadablePeriod validPeriod = new StatsRequest().setPeriodExpression(winnerPeriod).getPeriod();
        for (String user : users) {
            final VoteStats voteStats = voteCalculator.calculateVoteStatsForUser(user, validPeriod);
            stats.addVoteStats(user, voteStats);
        }
        stats.sort();
        stats.setValidity(validPeriod);
        mongoDBStatsRepository.save(stats);

        boolean winner = true;
        for (Map.Entry<String, VoteStats> userVoteStats : stats.getVoteStats().entrySet()) {
            final User user = mongoDBUserRepository.findOneByNickName(userVoteStats.getKey());
            if (user != null) {
                final VoteStats voteStats = userVoteStats.getValue();
                final Context context = new Context();
                String greeting;
                if (winner) {
                    greeting = "Congratulations " + user.getGivenName() + " " + user.getSurName() + ", you have been voted the most sarcastic person of the week!";
                    winner = false;
                } else {
                    greeting = "Hello " + user.getGivenName() + " " + user.getSurName();
                }
                context.setVariable("greeting", greeting);
                context.setVariable("stats", voteStats);
                context.setVariable("from", stats.getStart());
                context.setVariable("until", stats.getEnd());

                final String email = user.getEmail();
                final String htmlContent = springTemplateEngine.process("email", context);
                LOGGER.debug(htmlContent);

                final MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(fromEmailAddress);
                message.setTo(email);
                message.setSubject(subject);
                message.setText(htmlContent, true);
                mailSender.send(mimeMessage);
            } else {
                LOGGER.error("Encountered unknown user (" + userVoteStats.getKey() + ") while calculating winner!");
            }
        }

    }
}