package com.travelbot.TravBot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.model.objectmapper.ModelObjectMapper;
import com.travelbot.TravBot.model.EventsModel;
import com.travelbot.TravBot.service.BotService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class BotController {
    @Autowired
    @Qualifier("lineMessagingClient")
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private BotService botService;

    @Autowired
    @Qualifier("lineSignatureValidator")
    private LineSignatureValidator lineSignatureValidator;

    @RequestMapping(value="/webhook", method= RequestMethod.POST)
    public ResponseEntity<String> callback(
            @RequestHeader("X-Line-Signature") String xLineSignature,
            @RequestBody String eventsPayload)
    {
        try {
            if (!lineSignatureValidator.validateSignature(eventsPayload.getBytes(), xLineSignature)) {
                throw new RuntimeException("Invalid Signature Validation");
            }

            // parsing event
            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
            EventsModel eventsModel = objectMapper.readValue(eventsPayload, EventsModel.class);

            eventsModel.getEvents().forEach((event)->{
                if (event instanceof MessageEvent){
                    if (event.getSource() instanceof GroupSource || event.getSource() instanceof RoomSource) {
//                        handleGroupRoomChats((MessageEvent) event);
                    } else {
                        handleOneOnOneChats((MessageEvent) event);
                    }
                }
            });

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private void handleOneOnOneChats(MessageEvent event) {
        if  (event.getMessage() instanceof AudioMessageContent
                || event.getMessage() instanceof ImageMessageContent
                || event.getMessage() instanceof VideoMessageContent
                || event.getMessage() instanceof FileMessageContent
        ) {
            handleContentMessage(event);
        } else if(event.getMessage() instanceof TextMessageContent) {
            handleTextMessage(event);
        } else {
            botService.replyText(event.getReplyToken(), "Unknown Message");
        }
    }

    private void handleContentMessage(MessageEvent event) {
        String baseURL     = "https://alexbot-line-java.herokuapp.com";
        String contentURL  = baseURL+"/content/"+ ((MessageEvent) event).getMessage().getId();
        String contentType = ((MessageEvent) event).getMessage().getClass().getSimpleName();
        String textMsg     = contentType.substring(0, contentType.length() -14)
                + " yang kamu kirim bisa diakses dari link:\n "
                + contentURL;

        botService.replyText(((MessageEvent) event).getReplyToken(), textMsg);
    }

    private void handleTextMessage(MessageEvent event) {
        TextMessageContent textMessageContent = (TextMessageContent) event.getMessage();

        if (textMessageContent.getText().toLowerCase().contains("flex")) {
//            replyFlexMessage(event.getReplyToken());
        } else {
            botService.replyText(event.getReplyToken(), textMessageContent.getText());
        }
    }

//    private void replyFlexMessage(String replyToken) {
//        try {
//            ClassLoader classLoader = getClass().getClassLoader();
//            String flexTemplate = IOUtils.toString(classLoader.getResourceAsStream("flex_message.json"));
//
//            ObjectMapper objectMapper = ModelObjectMapper.createNewObjectMapper();
//            FlexContainer flexContainer = objectMapper.readValue(flexTemplate, FlexContainer.class);
//
//            ReplyMessage replyMessage = new ReplyMessage(replyToken, new FlexMessage("Dicoding Academy", flexContainer));
//            botService.reply(replyMessage);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
