package dev.darealturtywurty.superturtybot.commands.economy.promotion;

import dev.darealturtywurty.superturtybot.TurtyBot;
import dev.darealturtywurty.superturtybot.core.api.ApiHandler;
import dev.darealturtywurty.superturtybot.core.util.Constants;
import dev.darealturtywurty.superturtybot.core.util.MathUtils;
import dev.darealturtywurty.superturtybot.core.util.function.Either;
import dev.darealturtywurty.superturtybot.database.pojos.collections.Economy;
import dev.darealturtywurty.superturtybot.modules.economy.EconomyManager;
import io.javalin.http.HttpStatus;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CodePromotionMinigame implements PromotionMinigame {
    private static final int CODE_SNIPPET_CHUNKS = 3;
    private static final int MAX_LOCAL_SNIPPET_ATTEMPTS = 12;
    private static final int MIN_NON_WHITESPACE_CHARS = 80;
    private static final double MAX_COMMENT_RATIO = 0.6;

    @Override
    public void start(SlashCommandInteractionEvent event, Economy account) {
        event.getHook().editOriginal("🔎 Finding a code snippet for your promotion...").queue();
        CompletableFuture
                .supplyAsync(() -> Code.findCode(attempt -> event.getHook()
                        .editOriginalFormat("🔎 Finding a code snippet for your promotion... (try %d/%d)",
                                attempt, MAX_LOCAL_SNIPPET_ATTEMPTS)
                        .queue()))
                .whenComplete((code, throwable) -> {
                    if (throwable != null) {
                        Constants.LOGGER.error("Failed to start code guesser promotion.", throwable);
                        event.getHook()
                                .editOriginal("❌ Could not start the promotion minigame right now. Please try again later.")
                                .queue();
                        return;
                    }

                    event.getHook()
                            .editOriginal("✅ You have started the promotion minigame! You have 15 seconds to guess the programming language.")
                            .flatMap(message -> message.createThreadChannel(event.getUser().getName() + "'s Promotion"))
                            .queue(channel -> {
                                channel.addThreadMember(event.getUser()).queue();
                                channel.sendMessageFormat(
                                                "Guess the programming language of the following code to get promoted to the next job level!\n\n```\n%s```",
                                                code.code().substring(0, MathUtils.clamp(code.code().length(), 0, 1900)))
                                        .queue(message -> TurtyBot.EVENT_WAITER.builder(MessageReceivedEvent.class)
                                                .condition(e -> e.getChannel().getIdLong() == channel.getIdLong()
                                                        && e.getAuthor().getIdLong() == event.getUser().getIdLong())
                                                .timeout(15, TimeUnit.SECONDS)
                                                .timeoutAction(() -> {
                                                    channel.sendMessageFormat("❌ You took too long to answer! The correct answer was %s.",
                                                                    code.language().name())
                                                            .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                                    account.setReadyForPromotion(false);
                                                    EconomyManager.updateAccount(account);
                                                })
                                                .success(messageEvent -> {
                                                    if (Code.matchesLanguage(code.language().name(),
                                                            messageEvent.getMessage().getContentRaw())) {
                                                        channel.sendMessageFormat("✅ You have been promoted to level %d!",
                                                                        account.getJobLevel() + 1)
                                                                .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                                        account.setJobLevel(account.getJobLevel() + 1);
                                                    } else {
                                                        channel.sendMessageFormat("❌ That is not the correct answer! The correct answer was %s.",
                                                                        code.language().name())
                                                                .queue(ignored -> channel.getManager().setArchived(true).setLocked(true).queue());
                                                    }

                                                    account.setReadyForPromotion(false);
                                                    EconomyManager.updateAccount(account);
                                                }).build());
                            });
                });
    }

    public record Code(String code, Language language) {
        public record Language(String name, String extension) {
        }

        private record SnippetRow(String snippet, String language, String repoFileName, String githubRepoUrl,
                                  String commitHash, int startingLineNumber) {
        }

        private enum AllowedLanguage {
            BASH("Bash", "bash"),
            C("C", "c"),
            CPP("C++", "c++", "cpp", "cxx"),
            GO("Go", "go", "golang"),
            HTML("HTML", "html", "htm"),
            JSON("JSON", "json"),
            JAVA("Java", "java"),
            JAVASCRIPT("JavaScript", "javascript", "js", "node", "nodejs"),
            JUPYTER("Jupyter", "jupyter", "ipynb"),
            POWERSHELL("PowerShell", "powershell", "pwsh", "ps"),
            PYTHON("Python", "python", "py"),
            RUBY("Ruby", "ruby", "rb"),
            RUST("Rust", "rust", "rs"),
            SHELL("Shell", "shell", "sh"),
            YAML("YAML", "yaml", "yml");

            private final String displayName;
            private final Set<String> aliases;

            AllowedLanguage(String displayName, String... aliases) {
                this.displayName = displayName;
                this.aliases = new HashSet<>();
                for (String alias : aliases) {
                    this.aliases.add(alias.toLowerCase(Locale.ROOT));
                }
            }

            private boolean matches(String value) {
                if (value == null || value.isBlank())
                    return false;

                return aliases.contains(value.toLowerCase(Locale.ROOT));
            }

            private static Optional<AllowedLanguage> fromValue(String value) {
                if (value == null)
                    return Optional.empty();

                for (AllowedLanguage language : values()) {
                    if (language.matches(value))
                        return Optional.of(language);
                }

                return Optional.empty();
            }
        }

        public static Code findCode() throws IllegalStateException {
            return findCode(null);
        }

        public static Code findCode(Consumer<Integer> onAttempt) throws IllegalStateException {
            Optional<Code> localCode = findLocalCode(onAttempt);
            if (localCode.isPresent())
                return localCode.get();

            Either<Code, HttpStatus> response = ApiHandler.findCode();
            if (response.isLeft()) {
                Code code = response.getLeft();
                return normalizeLanguage(code).orElseThrow(() -> new IllegalStateException(
                        "Language is not in allowed list: " + code.language().name()));
            }

            throw new IllegalStateException("Could not find code! Status code: " + response.getRight().getCode());
        }

        private static Optional<Code> findLocalCode(Consumer<Integer> onAttempt) {
            Path databasePath = Paths.get("snippets.db");
            if (!Files.exists(databasePath))
                return Optional.empty();

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath())) {
                long maxId = fetchMaxId(connection);
                if (maxId <= 0)
                    return Optional.empty();

                for (int attempt = 0; attempt < MAX_LOCAL_SNIPPET_ATTEMPTS; attempt++) {
                    if (onAttempt != null)
                        onAttempt.accept(attempt + 1);

                    AllowedLanguage targetLanguage = pickRandomAllowedLanguage();
                    if (targetLanguage == null)
                        break;

                    long randomId = ThreadLocalRandom.current().nextLong(1, maxId + 1);
                    SnippetRow row = fetchSnippetRowByLanguage(connection, targetLanguage, randomId);
                    if (row == null)
                        row = fetchFirstSnippetRowByLanguage(connection, targetLanguage);
                    if (row == null)
                        continue;

                    Code code = assembleSnippet(connection, row);
                    if (code != null && !isCommentHeavy(code.code())) {
                        Optional<Code> normalized = normalizeLanguage(code);
                        if (normalized.isPresent())
                            return normalized;
                    }
                }

                SnippetRow firstRow = fetchFirstSnippetRow(connection);
                if (firstRow == null)
                    return Optional.empty();

                Code fallback = assembleSnippet(connection, firstRow);
                if (fallback != null && !isCommentHeavy(fallback.code()))
                    return normalizeLanguage(fallback);

                return Optional.empty();
            } catch (SQLException exception) {
                Constants.LOGGER.error("Failed to read code snippets database!", exception);
                return Optional.empty();
            }
        }

        private static long fetchMaxId(Connection connection) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("SELECT MAX(id) FROM snippets");
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next())
                    return 0L;

                return resultSet.getLong(1);
            }
        }

        private static SnippetRow fetchSnippetRowByLanguage(Connection connection, AllowedLanguage language, long id)
                throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT snippet, language, repo_file_name, github_repo_url, commit_hash, starting_line_number
                            FROM snippets
                            WHERE language = ? COLLATE NOCASE
                              AND id >= ?
                            ORDER BY id
                            LIMIT 1
                            """)) {
                statement.setString(1, language.displayName);
                statement.setLong(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next())
                        return null;

                    return new SnippetRow(
                            resultSet.getString("snippet"),
                            resultSet.getString("language"),
                            resultSet.getString("repo_file_name"),
                            resultSet.getString("github_repo_url"),
                            resultSet.getString("commit_hash"),
                            resultSet.getInt("starting_line_number"));
                }
            }
        }

        private static SnippetRow fetchFirstSnippetRowByLanguage(Connection connection, AllowedLanguage language)
                throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT snippet, language, repo_file_name, github_repo_url, commit_hash, starting_line_number
                            FROM snippets
                            WHERE language = ? COLLATE NOCASE
                            ORDER BY id
                            LIMIT 1
                            """)) {
                statement.setString(1, language.displayName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next())
                        return null;

                    return new SnippetRow(
                            resultSet.getString("snippet"),
                            resultSet.getString("language"),
                            resultSet.getString("repo_file_name"),
                            resultSet.getString("github_repo_url"),
                            resultSet.getString("commit_hash"),
                            resultSet.getInt("starting_line_number"));
                }
            }
        }

        private static SnippetRow fetchFirstSnippetRow(Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("""
                         SELECT snippet, language, repo_file_name, github_repo_url, commit_hash, starting_line_number
                         FROM snippets
                         ORDER BY id
                         LIMIT 1
                         """)) {
                if (!resultSet.next())
                    return null;

                return new SnippetRow(
                        resultSet.getString("snippet"),
                        resultSet.getString("language"),
                        resultSet.getString("repo_file_name"),
                        resultSet.getString("github_repo_url"),
                        resultSet.getString("commit_hash"),
                        resultSet.getInt("starting_line_number"));
            }
        }

        private static Code assembleSnippet(Connection connection, SnippetRow seed) throws SQLException {
            List<String> snippets = fetchSnippetChunks(connection, seed);
            if (snippets.isEmpty())
                return null;

            String combined = String.join("\n", snippets);
            return new Code(combined, new Language(seed.language(), ""));
        }

        private static List<String> fetchSnippetChunks(Connection connection, SnippetRow seed) throws SQLException {
            List<String> snippets = fetchSnippetChunksFromStart(connection, seed, true);
            if (snippets.size() >= CODE_SNIPPET_CHUNKS)
                return snippets;

            List<String> fromBefore = fetchSnippetChunksFromStart(connection, seed, false);
            Collections.reverse(fromBefore);

            LinkedHashSet<String> merged = new LinkedHashSet<>();
            merged.addAll(fromBefore);
            merged.addAll(snippets);

            List<String> mergedList = new ArrayList<>(merged);
            if (mergedList.size() > CODE_SNIPPET_CHUNKS)
                return mergedList.subList(0, CODE_SNIPPET_CHUNKS);

            return mergedList;
        }

        private static List<String> fetchSnippetChunksFromStart(Connection connection, SnippetRow seed,
                                                                boolean forward) throws SQLException {
            String order = forward ? "ASC" : "DESC";
            String lineComparator = forward ? ">=" : "<=";
            String query = """
                    SELECT snippet
                    FROM snippets
                    WHERE repo_file_name IS ?
                      AND github_repo_url IS ?
                      AND commit_hash IS ?
                      AND starting_line_number %s ?
                    ORDER BY starting_line_number %s
                    LIMIT ?
                    """.formatted(lineComparator, order);

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setObject(1, seed.repoFileName());
                statement.setObject(2, seed.githubRepoUrl());
                statement.setObject(3, seed.commitHash());
                statement.setInt(4, seed.startingLineNumber());
                statement.setInt(5, CODE_SNIPPET_CHUNKS);

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<String> snippets = new ArrayList<>();
                    while (resultSet.next()) {
                        snippets.add(resultSet.getString("snippet"));
                    }

                    return snippets;
                }
            }
        }

        private static boolean isCommentHeavy(String snippet) {
            int nonWhitespaceChars = 0;
            int nonEmptyLines = 0;
            int commentLines = 0;
            boolean inBlockComment = false;
            boolean inHtmlComment = false;

            for (String line : snippet.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty())
                    continue;

                nonEmptyLines++;
                nonWhitespaceChars += trimmed.replaceAll("\\s+", "").length();

                if (inBlockComment) {
                    commentLines++;
                    if (trimmed.contains("*/"))
                        inBlockComment = false;
                    continue;
                }

                if (inHtmlComment) {
                    commentLines++;
                    if (trimmed.contains("-->"))
                        inHtmlComment = false;
                    continue;
                }

                if (trimmed.startsWith("/*")) {
                    commentLines++;
                    if (!trimmed.contains("*/"))
                        inBlockComment = true;
                    continue;
                }

                if (trimmed.startsWith("<!--")) {
                    commentLines++;
                    if (!trimmed.contains("-->"))
                        inHtmlComment = true;
                    continue;
                }

                if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--")
                        || trimmed.startsWith("*") || trimmed.startsWith(";")) {
                    commentLines++;
                }
            }

            if (nonEmptyLines == 0)
                return true;

            if (nonWhitespaceChars < MIN_NON_WHITESPACE_CHARS)
                return true;

            return (commentLines / (double) nonEmptyLines) > MAX_COMMENT_RATIO;
        }

        private static Optional<Code> normalizeLanguage(Code code) {
            Optional<AllowedLanguage> allowed = AllowedLanguage.fromValue(code.language().name());
            if (allowed.isEmpty())
                return Optional.empty();

            AllowedLanguage language = allowed.get();
            return Optional.of(new Code(code.code(), new Language(language.displayName, code.language().extension())));
        }

        private static boolean matchesLanguage(String expected, String provided) {
            Optional<AllowedLanguage> expectedLanguage = AllowedLanguage.fromValue(expected);
            Optional<AllowedLanguage> providedLanguage = AllowedLanguage.fromValue(provided);
            return expectedLanguage.isPresent()
                    && providedLanguage.isPresent()
                    && expectedLanguage.get() == providedLanguage.get();
        }

        private static AllowedLanguage pickRandomAllowedLanguage() {
            AllowedLanguage[] values = AllowedLanguage.values();
            if (values.length == 0)
                return null;

            return values[ThreadLocalRandom.current().nextInt(values.length)];
        }
    }
}
