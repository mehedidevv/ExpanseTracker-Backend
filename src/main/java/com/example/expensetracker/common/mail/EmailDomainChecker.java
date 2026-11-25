package com.example.expensetracker.common.mail;

import java.util.Hashtable;
import java.util.Locale;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DNS check that an email's domain exists and can receive mail (has an MX record, or an A/AAAA record
 * as implicit MX). Catches typos like {@code gmial.com} / {@code gmail.con} before anything is saved.
 * If DNS itself is unreachable the address is accepted (we never block sign-up because of our network).
 */
@Component
public class EmailDomainChecker {

    private static final Logger log = LoggerFactory.getLogger(EmailDomainChecker.class);

    private final MailSettings settings;

    public EmailDomainChecker(MailSettings settings) {
        this.settings = settings;
    }

    public boolean canReceiveMail(String email) {
        if (!settings.verifyDomain()) {
            return true;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            // One record type per query: some home routers reject multi-type queries.
            Attribute mx = lookup(ctx, domain, "MX");
            if (mx != null && mx.size() > 0) {
                // RFC 7505 "null MX" (0 .) means the domain explicitly accepts no mail
                return !(mx.size() == 1 && String.valueOf(mx.get()).trim().matches("0\\s+\\.?"));
            }
            return lookup(ctx, domain, "A") != null || lookup(ctx, domain, "AAAA") != null; // implicit MX
        } catch (NameNotFoundException e) {
            return false; // domain does not exist
        } catch (NamingException e) {
            log.warn("DNS check for {} failed ({}); accepting the address", domain, e.getMessage());
            return true;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // nothing to do
                }
            }
        }
    }

    private static Attribute lookup(DirContext ctx, String domain, String type) throws NamingException {
        Attributes attrs = ctx.getAttributes(domain, new String[] {type});
        Attribute attr = attrs.get(type);
        return attr == null || attr.size() == 0 ? null : attr;
    }
}
