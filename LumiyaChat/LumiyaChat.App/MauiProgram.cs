using LumiyaChat.Core;
using Microsoft.Extensions.DependencyInjection;

namespace LumiyaChat.App;

public static class MauiProgram
{
	public static MauiApp CreateMauiApp()
	{
		var builder = MauiApp.CreateBuilder();
		builder
			.UseMauiApp<App>()
			.ConfigureFonts(fonts =>
			{
				fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
				fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
			});

		// Services and view models
		builder.Services.AddSingleton<IChatService, LibreMetaverseChatService>();
		builder.Services.AddSingleton<LoginViewModel>();
		builder.Services.AddSingleton<ChatViewModel>();

		// Pages
		builder.Services.AddSingleton<LoginPage>();
		builder.Services.AddSingleton<ChatPage>();

		return builder.Build();
	}
}
