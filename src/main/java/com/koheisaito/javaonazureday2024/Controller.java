package com.koheisaito.javaonazureday2024;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.assistants.AssistantsClient;
import com.azure.ai.openai.assistants.AssistantsClientBuilder;
import com.azure.ai.openai.assistants.models.Assistant;
import com.azure.ai.openai.assistants.models.AssistantCreationOptions;
import com.azure.ai.openai.assistants.models.AssistantThread;
import com.azure.ai.openai.assistants.models.AssistantThreadCreationOptions;
import com.azure.ai.openai.assistants.models.CodeInterpreterToolDefinition;
import com.azure.ai.openai.assistants.models.MessageImageFileContent;
import com.azure.ai.openai.assistants.models.MessageRole;
import com.azure.ai.openai.assistants.models.MessageTextContent;
import com.azure.ai.openai.assistants.models.MessageTextDetails;
import com.azure.ai.openai.assistants.models.PageableList;
import com.azure.ai.openai.assistants.models.RunStatus;
import com.azure.ai.openai.assistants.models.ThreadMessage;
import com.azure.ai.openai.assistants.models.ThreadRun;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.ai.openai.models.ImageGenerationData;
import com.azure.ai.openai.models.ImageGenerationOptions;
import com.azure.ai.openai.models.ImageGenerations;
import com.azure.ai.openai.models.SpeechGenerationOptions;
import com.azure.ai.openai.models.SpeechVoice;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.koheisaito.javaonazureday2024.model.RequestBodySpeech;

@RestController
public class Controller {
    
    // Azure OpenAI API Key and Endpoint        
    @Value("${aoai.api.key.eastus}")
    private String apiKeyEastUs;

    @Value("${aoai.api.endpoint.eastus}")
    private String apiEndpointEastUs;

    @Value("${aoai.api.key.northcentralus}")
    private String apiKeyNorthCentralUs;

    @Value("${aoai.api.endpoint.northcentralus}")
    private String apiEndpointNorthCentralUs;

    @Value("${aoai.api.key.eastus2}")
    private String apiKeyEastUs2;

    @Value("${aoai.api.endpoint.eastus2}")
    private String apiEndpointEastUs2;

    // Build OpenAI Client
    private OpenAIClient buildOpenAIClientEastUs() {
        return new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(apiKeyEastUs))
            .endpoint(apiEndpointEastUs)
            .buildClient();
    }

    private OpenAIClient buildOpenAIClientNorthCentralUs() {
        return new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(apiKeyNorthCentralUs))
            .endpoint(apiEndpointNorthCentralUs)
            .buildClient();
    }

    private AssistantsClient buildAssistantsClientEastUs2() {
        return new AssistantsClientBuilder()
            .credential(new AzureKeyCredential(apiKeyEastUs2))
            .endpoint(apiEndpointEastUs2)
            .buildClient();
    }

    @GetMapping("/chat")
    public void chat(@RequestParam String message) {
        OpenAIClient client = buildOpenAIClientEastUs();
		List<ChatRequestMessage> chatMessages = new ArrayList<>();
		chatMessages.add(new ChatRequestSystemMessage("あなたは役に立つアシスタントです。海賊のような口調で話してください。"));
		chatMessages.add(new ChatRequestUserMessage("お願いできますか？"));
		chatMessages.add(new ChatRequestAssistantMessage("喜んで！何をお手伝いしましょう？"));
		chatMessages.add(new ChatRequestUserMessage(message));

        ChatCompletions chatCompletions = client.getChatCompletions("gpt-4o", new ChatCompletionsOptions(chatMessages));

		System.out.printf("Model ID=%s is created at %s.%n", chatCompletions.getId(), chatCompletions.getCreatedAt());
		for (ChatChoice choice : chatCompletions.getChoices()) {
			ChatResponseMessage chatResponseMessage = choice.getMessage();
			System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), chatResponseMessage.getRole());
			System.out.println("Message:");
			System.out.println(chatResponseMessage.getContent());
		}
    }

    @GetMapping("/image")
    public void image(@RequestParam String message) {
		OpenAIClient client = buildOpenAIClientEastUs();
		ImageGenerationOptions imageGenerationOptions = new ImageGenerationOptions(message);
		ImageGenerations images = client.getImageGenerations("dall-e-3", imageGenerationOptions);

		for (ImageGenerationData imageGenerationData : images.getData()) {
			System.out.printf(
				"Image location URL that provides temporary access to download the generated image is %s.%n",
				imageGenerationData.getUrl());
		}
    }

	@PostMapping("/speech")
	public void speech(@RequestBody RequestBodySpeech body) throws IOException {
		OpenAIClient client = buildOpenAIClientNorthCentralUs();
		SpeechGenerationOptions options = new SpeechGenerationOptions(body.getMessage(), SpeechVoice.ALLOY);

		BinaryData speech = client.generateSpeechFromText("tts", options);
		// Checkout your generated speech in the file system.
		Path path = Paths.get("./outputs/speech.wav");
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
        }
		Files.write(path, speech.toBytes());
	}

    @GetMapping("/assistant")
    public void assistant(@RequestParam String message) throws InterruptedException {

        AssistantsClient client = buildAssistantsClientEastUs2();

        // Step 1: Assistant 作成
        Assistant assistant = client.createAssistant(
            new AssistantCreationOptions("gpt-4-turbo-2024-04-09")
            .setName("Math Teacher")  // Assistant の名前
            .setInstructions("あなたは数学の家庭教師です。コードを書いて実行し、数学の質問に答えてください。応答には、生成したコードも併せて回答してください。")  // Instructions の設定
            .setTools(Arrays.asList(new CodeInterpreterToolDefinition()))  // Tool の設定
        );

        // Step 2: Thread 作成
        AssistantThread thread = client.createThread(new AssistantThreadCreationOptions());

        // Step 3: Message を Thread に追加
        client.createMessage(thread.getId(), MessageRole.USER, message);

        // Step 4: Assistant 実行
        ThreadRun threadRun = client.createRun(thread, assistant);

        // Step 5: Run status 確認
        do {
            Thread.sleep(500);
            threadRun = client.getRun(thread.getId(), threadRun.getId());
            System.out.println("Run status: " + threadRun.getStatus());
        } while (threadRun.getStatus() == RunStatus.IN_PROGRESS || threadRun.getStatus() == RunStatus.QUEUED);

        // Step 6: Asssistant の応答を表示
        PageableList<ThreadMessage> messages = client.listMessages(thread.getId());
        for (ThreadMessage data : messages.getData()) {
            System.out.println("Message:");
            data.getContent().forEach(
                content -> {
                    if (content instanceof MessageTextContent) {
                        MessageTextDetails messageTextDetails = ((MessageTextContent) content).getText();
                        System.out.println(messageTextDetails.getValue());
                        messageTextDetails.getAnnotations().forEach(annotation ->
                            System.out.println("\tAnnotation start: " + annotation.getStartIndex()
                                + " ,end: " + annotation.getEndIndex() + " ,text: \"" + annotation.getText() + "\""));
                    } else if (content instanceof MessageImageFileContent) {
                        System.out.print("Image file ID: ");
                        System.out.println(((MessageImageFileContent) content).getImageFile().getFileId());
                    }
                }
            );
        }
    }
}
