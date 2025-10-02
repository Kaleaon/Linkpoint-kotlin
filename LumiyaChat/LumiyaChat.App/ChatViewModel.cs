using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using LumiyaChat.Core;

namespace LumiyaChat.App;

public sealed class ChatViewModel : INotifyPropertyChanged
{
	private readonly IChatService _chatService;
	private CancellationTokenSource? _cts;
	private string _outgoing = string.Empty;

	public event PropertyChangedEventHandler? PropertyChanged;

	public ObservableCollection<ChatMessage> Messages { get; } = new();
	public string Outgoing { get => _outgoing; set { _outgoing = value; OnPropertyChanged(); } }
	public ICommand SendCommand { get; }

	public ChatViewModel(IChatService chatService)
	{
		_chatService = chatService;
		SendCommand = new Command(async () => await SendAsync());
		_cts = new CancellationTokenSource();
		_ = ListenAsync(_cts.Token);
	}

	private async Task ListenAsync(CancellationToken token)
	{
		await foreach (var msg in _chatService.GetMessagesAsync(token))
		{
			MainThread.BeginInvokeOnMainThread(() => Messages.Add(msg));
		}
	}

	private async Task SendAsync()
	{
		var text = Outgoing;
		if (string.IsNullOrWhiteSpace(text)) return;
		Outgoing = string.Empty;
		await _chatService.SendLocalChatAsync(text);
	}

	private void OnPropertyChanged([CallerMemberName] string? name = null) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
