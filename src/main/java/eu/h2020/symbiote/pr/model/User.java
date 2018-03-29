package eu.h2020.symbiote.pr.model;

import com.querydsl.core.annotations.QueryEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@QueryEntity
@Document
public class User {

    @Id
    private String id;
    private String name;
    private Integer age;

}