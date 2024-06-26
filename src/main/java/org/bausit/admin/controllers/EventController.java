package org.bausit.admin.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bausit.admin.dtos.CheckinRequest;
import org.bausit.admin.dtos.SecurityUser;
import org.bausit.admin.dtos.TokenResponse;
import org.bausit.admin.exceptions.EntityNotFoundException;
import org.bausit.admin.exceptions.InvalidRequestException;
import org.bausit.admin.models.Event;
import org.bausit.admin.models.Participant;
import org.bausit.admin.models.Team;
import org.bausit.admin.services.EventService;
import org.bausit.admin.services.ParticipantService;
import org.bausit.admin.services.PdfService;
import org.bausit.admin.services.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Log4j2
public class EventController {
    private final EventService eventService;
    private final PdfService pdfService;
    private final ParticipantService participantService;
    private final TokenService tokenService;

    @GetMapping("/today")
    public Map<Long, String> getTodayEvents() {
        return eventService.findTodayEvent()
            .stream()
            .collect(Collectors.toMap(Event::getId, Event::getName));
    }

    @PostMapping("/{eventId}/checkin")
    public TokenResponse checkin(@PathVariable long eventId, @RequestBody CheckinRequest request) {
        Event event = eventService.findById(eventId);

        try {
            Participant participant = participantService.findByEmailOrPhone(request.getEmailOrPhone());
            log.info("Found participant: {}", participant.getId());
            var token = tokenService.generateToken(new SecurityUser(participant));

            Instant checkoutDate = request.getCheckoutDate();
            try {
                eventService.checkin(event, participant, checkoutDate);
            } catch (DataIntegrityViolationException e) {
                log.info("Already checkin to this event");
            }

            return token;
        } catch (Exception e) {
            log.info("error: " + e.getMessage(), e);
            throw new InvalidRequestException(e.getMessage(), e);
        }
    }

    @GetMapping("/{eventId}")
    public Event getEvent(@PathVariable long eventId) {
        Event event = eventService.findById(eventId);
        event.initViewMode();
        return event;
    }

    @GetMapping("/{eventId}/participants")
    public Set<Participant> getParticipants(@PathVariable long eventId) {
        Event event = eventService.findById(eventId);
        Set<Participant> participants = event.getAllParticipants();
        participants.forEach(Participant::initViewMode);
        return participants;
    }

    @GetMapping
    public Iterable<Event> search(@RequestParam(required = false) String query) {
        log.info("search keywords: {}", query);

        Iterable<Event> events = eventService.query(query);
        events.forEach(Event::initViewMode);
        return events;
    }

    @PostMapping("/invite/{eventId}")
    public void invite(@PathVariable long eventId, @RequestBody List<Long> participants) {
        Event event = eventService.findById(eventId);
        eventService.invite(event, participants);
    }

    @DeleteMapping("/{eventId}")
    public void delete(@PathVariable long eventId) {
        eventService.delete(eventId);
    }

    @DeleteMapping ("/teams/{teamId}")
    public void deleteTeam(@PathVariable long teamId) {
        eventService.deleteTeam(teamId);
    }

    @GetMapping("/export/{eventId}")
    public void export(@PathVariable long eventId,
                       HttpServletResponse response) throws IOException {
        Event event = eventService.findById(eventId);

        response.setHeader("Expires", "0");
        response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "public");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + event.getId() + ".pdf" + "\"");

        OutputStream os = response.getOutputStream();
        pdfService.export(event, os);
        os.flush();
        os.close();
    }

    @PostMapping("/{eventId}/{teamId}")
    public void assignTeamMember(@PathVariable long eventId,
                                 @PathVariable long teamId,
                                 @RequestBody List<Long> participants) {
        Event event = eventService.findById(eventId);
        Team team = eventService.findTeam(event, teamId);
        eventService.assignTeamMember(event, team, participants);
    }

    @DeleteMapping ("/{eventId}/{teamId}/{participantId}")
    public void removeTeamMember(@PathVariable long eventId,
                                 @PathVariable long teamId,
                                 @PathVariable long participantId) {
        log.debug("deleting");
        Event event = eventService.findById(eventId);
        Team team = eventService.findTeam(event, teamId);
        eventService.removeTeamMember(event, team, participantId)
            .orElseThrow(() -> new EntityNotFoundException("participant id: " + participantId +
                " is not in team: " + team.getName()));
    }
}
