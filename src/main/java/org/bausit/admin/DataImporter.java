package org.bausit.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bausit.admin.models.Participant;
import org.bausit.admin.services.ParticipantService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;

@Component
@Log4j2
@RequiredArgsConstructor
public class DataImporter implements CommandLineRunner {
    private final ParticipantService participantService;

    DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy")
        .withZone(ZoneId.systemDefault());
    SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");

    @Override
    public void run(String... args) throws Exception {
        InputStream is = new ClassPathResource("/sample-data.csv").getInputStream();
        Scanner sc = new Scanner(is, "UTF-8");

        String[] headers = sc.nextLine().split(",");
        for(int i=0; i<headers.length; i++) {
            log.info("{} - {}", i, headers[i]);
        }

        //skip header
        String line;
        while(sc.hasNextLine()) {
            line = sc.nextLine();
            String[] fields = line.split(",");
            log.info(line);
            Participant.Status status = fields.length > 12 && StringUtils.hasText(fields[12]) ?
                Participant.Status.valueOf(fields[12]) :
                Participant.Status.A;
            Instant issueDate = parseDate(fields, 1);
            String phoneNumber = fields.length > 11 ? fields[11] : "";
            String email = fields.length > 14 ? fields[14] : "";
            Instant birthDate = parseDate(fields, 15);
            String remark = fields.length > 17 ? fields[17] : "";
            String note = fields.length > 18 ? fields[18] : "";
            String emergencyContact = fields.length > 19 ? fields[19] : "";
            int birthYear = birthDate != null ? Integer.parseInt(yearFormatter.format(birthDate)) : 0;

            Participant p = Participant.builder()
                .memberNumber(Integer.parseInt(fields[0]))
                .issueDate(issueDate)
                .birthYear(birthYear)
                .lastName(fields[2])
                .firstName(fields[3])
                .chineseName(fields[4])
                .gender(Participant.Gender.valueOf(fields[5]))
                .type(Participant.Type.valueOf(fields[6]))
                .address(fields[7])
                .city(fields[8])
                .state(fields[9])
                .zipcode(fields[10])
                .phoneNumber(phoneNumber)
                .status(status)
                .email(email)
                .birthDate(birthDate)
                .remark(remark)
                .note(note)
                .emergencyContact(emergencyContact)
                .build();
            log.info(p);
            participantService.save(p);
        }
    }

    private Instant parseDate(String[] fields, int idx) {
        if(fields.length > idx && StringUtils.hasText(fields[idx])) {
            try {
                String dateString = fields[idx];
                String[] dateParts = dateString.split("\\/");
                if(dateParts[2].length() == 2) {
                    int year = Integer.parseInt(dateParts[2]);
                    int yearPrefix = 19;
                    if(year < 30)
                        yearPrefix = 20;
                    //prepend 19 if year has only 2 digits
                    dateString = dateParts[0] + "/" + dateParts[1] + "/" + yearPrefix + dateParts[2];
                }
                Date date = sdf.parse(dateString);
                Instant instant = date.toInstant();
                return instant;
            } catch (Exception e) {
                log.info("invalid date string: {}", fields[idx]);
            }
        }

        return null;
    }
}
