///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import com.jcraft.jsch.Session;

import java.util.Optional;

public class GitPushWithDebug {
    public static void main(String... args) throws Exception {
        // Enable JGit debug logging
        System.setProperty("org.eclipse.jgit.util.Debug", "true");
        System.setProperty("org.eclipse.jgit.transport.logging", "ALL");

        // Fetch username and password from environment variables
        String gitUsername = Optional.ofNullable(System.getenv("GIT_EMAIL")).orElse("");
        String gitPassword = Optional.ofNullable(System.getenv("GIT_PW")).orElse("");
        
        // Create CredentialsProvider with username and password
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(gitUsername, gitPassword);

        try (Git git = Git.open(new java.io.File("."))) {
            // Configure the push command
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(credentialsProvider);

            // Transport config callback for handling SSH transport
            pushCommand.setTransportConfigCallback(new TransportConfigCallback() {
                @Override
                public void configure(org.eclipse.jgit.transport.Transport transport) {
                    if (transport instanceof SshTransport) {
                        // Custom SSH session factory
                        ((SshTransport) transport).setSshSessionFactory(new JschConfigSessionFactory() {
                            @Override
                            protected void configure(com.jgit.transport.OpenSshConfig.Host host, Session session) {
                                session.setConfig("StrictHostKeyChecking", "no");
                                session.setConfig("PreferredAuthentications", "publickey");
                            }

                            @Override
                            protected com.jcraft.jsch.JSch createDefaultJSch(org.eclipse.jgit.util.FS fs) throws com.jcraft.jsch.JSchException {
                                com.jcraft.jsch.JSch jsch = super.createDefaultJSch(fs);
                                String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa";
                                jsch.addIdentity(privateKeyPath);
                                System.out.println("Using private key: " + privateKeyPath);
                                return jsch;
                            }
                        });
                    }
                }
            });

            // Perform the push operation
            pushCommand.call();
            System.out.println("Push successful.");
        } catch (GitAPIException e) {
            System.err.println("Git Push failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
