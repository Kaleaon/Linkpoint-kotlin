using System.Collections.Concurrent;
using OpenMetaverse;

namespace LumiyaChat.Core;

public sealed class LibreMetaverseChatService : IChatService
{
	private readonly GridClient _client;
	private readonly ConcurrentQueue<ChatMessage> _messageQueue = new();
	private readonly AsyncAutoResetEvent _messageAvailable = new();
	private string _loginUri = "https://login.agni.lindenlab.com/cgi-bin/login.cgi";

	public LibreMetaverseChatService()
	{
		_client = new GridClient();
		_client.Settings.MULTIPLE_SIMS = false;
		_client.Settings.STORE_LAND_PATCHES = false;
		_client.Settings.ALWAYS_DECODE_OBJECTS = false;
		_client.Settings.SEND_AGENT_UPDATES = true;


	}

	public Task InitializeAsync(string loginUri)
	{
		if (!string.IsNullOrWhiteSpace(loginUri))
		{
			_loginUri = loginUri.Trim();
		}
		return Task.CompletedTask;
	}

	public Task LoginAsync(string firstName, string lastName, string password)
	{
		var loginParams = _client.Network.DefaultLoginParams(
			firstName,
			lastName,
			password,
			"LumiyaChat",
			"0.1"
		);
		loginParams.URI = _loginUri;
		var success = _client.Network.Login(loginParams);
		if (!success)
		{
			throw new InvalidOperationException("Login failed.");
		}
		Enqueue(new ChatMessage { Text = "Login successful.", FromName = "System", Channel = ChatChannel.Local });
		return Task.CompletedTask;
	}

	public Task SendLocalChatAsync(string message)
	{
		_client.Self.Chat(message, 0, ChatType.Normal);
		return Task.CompletedTask;
	}

	public async IAsyncEnumerable<ChatMessage> GetMessagesAsync([System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken)
	{
		while (!cancellationToken.IsCancellationRequested)
		{
			while (_messageQueue.TryDequeue(out var msg))
			{
				yield return msg;
			}
			await _messageAvailable.WaitAsync(cancellationToken).ConfigureAwait(false);
		}
	}

	// TODO: wire simulator chat/IM events when confirming event types in installed package

	private void Enqueue(ChatMessage message)
	{
		_messageQueue.Enqueue(message);
		_messageAvailable.Set();
	}

	private sealed class AsyncAutoResetEvent
	{
		private readonly SemaphoreSlim _semaphore = new(0, 1);
		public void Set()
		{
			if (_semaphore.CurrentCount == 0)
			{
				_semaphore.Release();
			}
		}
		public async Task WaitAsync(CancellationToken cancellationToken)
		{
			await _semaphore.WaitAsync(cancellationToken).ConfigureAwait(false);
		}
	}
}
