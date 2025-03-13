///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r
//DEPS com.jcraft:jsch:0.1.55

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.transport.OpenSshConfig;

import java.util.Optional;

public class debugPush {
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

            // Configure SSH for pushing to the remote
            pushCommand.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport) {
                    URIish remoteUri = git.getRepository().getConfig().getRemote("origin").getURIs().iterator().next();
                    System.out.println("Using SSH for remote: " + remoteUri);

                    // Set SSH session factory to handle connections
                    ((SshTransport) transport).setSshSessionFactory(new JschConfigSessionFactory() {
                        @Override
                        protected void configure(OpenSshConfig.Host host, Session session) {
                            session.setConfig("StrictHostKeyChecking", "no"); // Disable host key checking
                            session.setConfig("PreferredAuthentications", "publickey"); // Use public key auth
                        }

                        @Override
                        protected JSch createDefaultJSch() throws com.jcraft.jsch.JSchException {
                            JSch jsch = super.createDefaultJSch();
                            String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa"; // Your private key path
                            jsch.addIdentity(privateKeyPath);
                            System.out.println("Using SSH private key: " + privateKeyPath); // Log private key path for debugging
                            return jsch;
                        }
                    });
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
