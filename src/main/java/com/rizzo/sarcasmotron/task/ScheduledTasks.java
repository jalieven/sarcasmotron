package com.rizzo.sarcasmotron.task;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
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
import java.util.Set;

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

        final Set<String> contestants = stats.getVoteStats().keySet();

        // send winner-mail
        final String winner = Iterables.getFirst(contestants, null);
        final User winningUser = mongoDBUserRepository.findOneByNickName(winner);
        final String winnerFullname = winningUser.getGivenName() + " " + winningUser.getSurName();
        final String winnerGreeting = "Congratulations " + winnerFullname
                + ", you have been voted the most sarcastic person of the week!";
        final VoteStats winnerVoteStats = stats.getVoteStats().get(winner);
        final Context winningContext = new Context();
        winningContext.setVariable("greeting", winnerGreeting);
        winningContext.setVariable("stats", winnerVoteStats);
        winningContext.setVariable("from", stats.getStart());
        winningContext.setVariable("until", stats.getEnd());
        sendMail(winningUser, winningContext, "winner-email");

        // send loser-mail
        final Iterable<String> losers = Iterables.filter(contestants, new Predicate<String>() {
            @Override
            public boolean apply(String nickName) {
                return !nickName.equals(winner);
            }
        });

        for (String loser : losers) {
            final User loserUser = mongoDBUserRepository.findOneByNickName(loser);
            final VoteStats loserVoteStats = stats.getVoteStats().get(loser);
            final String loserGreeting = "Hello " + loserUser.getGivenName() + " " + loserUser.getSurName();
            final Context loserContext = new Context();
            loserContext.setVariable("winner", winnerFullname);
            loserContext.setVariable("greeting", loserGreeting);
            loserContext.setVariable("stats", loserVoteStats);
            loserContext.setVariable("from", stats.getStart());
            loserContext.setVariable("until", stats.getEnd());
            sendMail(loserUser, loserContext, "loser-email");
        }

    }

    private void sendMail(User user, Context context, String template) throws MessagingException {
        final String email = user.getEmail();
        final String htmlContent = springTemplateEngine.process(template, context);
        LOGGER.debug(htmlContent);
        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
        message.setFrom(fromEmailAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(htmlContent, true);
        mailSender.send(mimeMessage);
    }

}