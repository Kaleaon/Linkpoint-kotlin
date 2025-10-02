using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using LumiyaChat.Core;

namespace LumiyaChat.App;

public sealed class LoginViewModel : INotifyPropertyChanged
{
	private readonly IChatService _chatService;
	private string _firstName = string.Empty;
	private string _lastName = string.Empty;
	private string _password = string.Empty;
	private string _loginUri = "https://login.agni.lindenlab.com/cgi-bin/login.cgi";
	private string _error = string.Empty;

	public event PropertyChangedEventHandler? PropertyChanged;

	public LoginViewModel(IChatService chatService)
	{
		_chatService = chatService;
		LoginCommand = new Command(async () => await LoginAsync());
	}

	public string FirstName { get => _firstName; set { _firstName = value; OnPropertyChanged(); } }
	public string LastName { get => _lastName; set { _lastName = value; OnPropertyChanged(); } }
	public string Password { get => _password; set { _password = value; OnPropertyChanged(); } }
	public string LoginUri { get => _loginUri; set { _loginUri = value; OnPropertyChanged(); } }
	public string Error { get => _error; set { _error = value; OnPropertyChanged(); } }

	public ICommand LoginCommand { get; }

	private async Task LoginAsync()
	{
		try
		{
			Error = string.Empty;
			await _chatService.InitializeAsync(LoginUri);
			await _chatService.LoginAsync(FirstName, LastName, Password);
			await Shell.Current.GoToAsync("//ChatPage");
		}
		catch (Exception ex)
		{
			Error = ex.Message;
		}
	}

	private void OnPropertyChanged([CallerMemberName] string? name = null) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
