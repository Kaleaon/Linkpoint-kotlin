namespace LumiyaChat.Core;

public interface IChatService
{
	Task InitializeAsync(string loginUri);
	Task LoginAsync(string firstName, string lastName, string password);
	Task SendLocalChatAsync(string message);
	IAsyncEnumerable<ChatMessage> GetMessagesAsync(CancellationToken cancellationToken);
}

public sealed class ChatMessage
{
	public DateTimeOffset Timestamp { get; init; } = DateTimeOffset.UtcNow;
	public string FromName { get; init; } = string.Empty;
	public Guid FromAgentId { get; init; } = Guid.Empty;
	public string Text { get; init; } = string.Empty;
	public ChatChannel Channel { get; init; } = ChatChannel.Local;
}

public enum ChatChannel
{
	Local,
	InstantMessage,
	Group
}
