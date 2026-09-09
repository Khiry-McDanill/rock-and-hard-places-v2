package com.rockandhardplaces.api;

import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.rockandhardplaces.account.*;
import com.rockandhardplaces.catalog.*;
import com.rockandhardplaces.portfolio.*;
import com.rockandhardplaces.credential.CredentialTrust;
import com.rockandhardplaces.credential.CredentialTrust.EvidenceKind;
import com.rockandhardplaces.credential.CredentialTrust.Status;

/** Owner-managed presentation never writes qualification or publication approval state. */
@RestController
@RequestMapping("/api/tradespeople/{id}")
@Transactional
class ProfessionalProfileController {
    private final ActiveAccountContext account;
    private final TradespersonRepository people;
    private final PersonTradeRepository qualifications;
    private final PersonSpecialtyRepository personSpecialties;
    private final SpecialtyRepository specialties;
    private final JdbcTemplate jdbc;
    private final PortfolioService portfolio;
    private final PortfolioItemRepository items;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final jakarta.validation.Validator validator;
    ProfessionalProfileController(ActiveAccountContext account, TradespersonRepository people,
            PersonTradeRepository qualifications, PersonSpecialtyRepository personSpecialties,
            SpecialtyRepository specialties, JdbcTemplate jdbc, PortfolioService portfolio, PortfolioItemRepository items, com.fasterxml.jackson.databind.ObjectMapper mapper, jakarta.validation.Validator validator) {
        this.account=account; this.people=people; this.qualifications=qualifications;
        this.personSpecialties=personSpecialties; this.specialties=specialties; this.jdbc=jdbc;
        this.portfolio=portfolio; this.items=items; this.mapper=mapper; this.validator=validator;
    }
    private Tradesperson owner(Long id) {
        if (!(account.activeProfile() instanceof Tradesperson actor) || !Objects.equals(actor.getId(), id))
            throw new SecurityException("Only the profile owner may make changes");
        Tradesperson person=people.findById(id).orElseThrow(ResourceNotFoundException::new);
        new AccountAuthorizationService().requireActive(person);
        return person;
    }
    record Business(@NotBlank @Size(max=200) String name, @NotNull @Size(max=5000) String description,
            @NotNull @Size(max=1000) String website, @NotNull @Size(max=100) String phone,
            @NotNull @Size(max=500) String address, @Min(0) @Max(300) Integer yearsInBusiness,
            @NotNull @Size(max=200) String role, @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=MediaField.class) JsonNode logoReference) {}
    record Identity(@NotBlank @Size(max=200) String displayName, @NotNull @Size(max=200) String headline,
            @NotNull @Size(max=5000) String bio, @Pattern(regexp="PERSON_FIRST|BUSINESS_FIRST") @NotNull String presentation,
            @Pattern(regexp="[0-9]{5}") @NotNull String baseZip, @NotNull @Min(0) @Max(1000) Integer serviceRadius,
            @NotNull AvailabilityStatus availabilityStatus, Long primaryTradeId,
            @NotNull @Size(max=100) List<Long> specialtyIds, @Valid Business business) {}
    @GetMapping("/professional-profile")
    Map<String,Object> details(@PathVariable Long id) {
        Tradesperson person=people.findById(id).orElseThrow(ResourceNotFoundException::new);
        boolean own=account.activeProfile() instanceof Tradesperson a && Objects.equals(a.getId(),id);
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("profile", ApiDtos.ProfileResponse.from(person));
        result.put("identity", jdbc.queryForList("SELECT headline,bio,presentation,primary_trade_id AS primaryTradeId FROM professional_profiles WHERE tradesperson_id=?",id).stream().findFirst().orElse(Map.of("headline","","bio","","presentation","PERSON_FIRST")));
        String businessColumns=own ? "name,description,website,phone,address,years_in_business AS yearsInBusiness,role,logo_reference AS logoReference" : "name,description,website,years_in_business AS yearsInBusiness,role,logo_reference AS logoReference";
        result.put("business",jdbc.queryForList("SELECT "+businessColumns+" FROM professional_businesses WHERE tradesperson_id=?",id).stream().findFirst().orElse(null));
        result.put("qualifications",qualifications.findByTradesperson(person).stream().map(q->Map.of("id",q.getTrade().getId(),"name",q.getTrade().getName())).toList());
        result.put("specialties",personSpecialties.findByTradesperson(person).stream().map(s->Map.of("id",s.getSpecialty().getId(),"name",s.getSpecialty().getName())).toList());
        result.put("credentials",jdbc.query("SELECT c.*,t.name AS confirmed_trade_name FROM professional_credentials c LEFT JOIN trades t ON t.id=c.confirmed_trade_id WHERE c.tradesperson_id=? ORDER BY c.id",(row,index)->{
            Map<String,Object> credential=new LinkedHashMap<>();
            for(String field:List.of("id","scope","type","name","issuer","jurisdiction")) credential.put(field,row.getObject(field));
            Status status=Status.valueOf(row.getString("verification_status"));
            EvidenceKind kind=EvidenceKind.valueOf(row.getString("evidence_kind"));
            Long trade=row.getObject("confirmed_trade_id")==null ? null : row.getLong("confirmed_trade_id");
            boolean supports=CredentialTrust.supportsQualification(status,row.getString("scope"),row.getString("type"),kind,trade);
            credential.put("verificationStatus",status); credential.put("evidenceKind",kind);
            credential.put("issuedDate",row.getString("issued_date")); credential.put("expirationDate",row.getString("expiration_date"));
            credential.put("verifiedAt",status==Status.VERIFIED ? row.getString("verified_at") : null);
            credential.put("supportsQualification",supports);
            credential.put("confirmedTradeName",supports ? row.getString("confirmed_trade_name") : null);
            if(own) {
                credential.put("number",row.getString("number")); credential.put("notes",row.getString("notes"));
                credential.put("evidenceReference",row.getString("evidence_reference"));
                credential.put("verificationBasis",row.getString("verification_basis"));
            }
            return credential;
        },id));
        result.put("credentialEvidenceUploadAvailable",false);
        return result;
    }
    @PutMapping("/professional-profile")
    Map<String,Object> save(@PathVariable Long id,@Valid @RequestBody Identity input) {
        Tradesperson person=owner(id);
        if(input.primaryTradeId()!=null && qualifications.findByTradesperson(person).stream().noneMatch(q->q.getTrade().getId().equals(input.primaryTradeId())))
            throw new IllegalArgumentException("Select an existing qualified trade");
        if(input.presentation().equals("BUSINESS_FIRST") && input.business()==null) throw new IllegalArgumentException("Business-first requires a business");
        List<Specialty> selected=new ArrayList<>();
        for(Long specialtyId:new LinkedHashSet<>(input.specialtyIds())) selected.add(specialties.findById(specialtyId).orElseThrow(ResourceNotFoundException::new));
        person.updateProfessionalIdentity(input.displayName().trim(),input.baseZip(),input.serviceRadius(),input.availabilityStatus());
        people.save(person);
        jdbc.update("INSERT INTO professional_profiles(tradesperson_id,headline,bio,presentation,primary_trade_id) VALUES(?,?,?,?,?) ON CONFLICT(tradesperson_id) DO UPDATE SET headline=excluded.headline,bio=excluded.bio,presentation=excluded.presentation,primary_trade_id=excluded.primary_trade_id",id,input.headline(),input.bio(),input.presentation(),input.primaryTradeId());
        personSpecialties.deleteAll(personSpecialties.findByTradesperson(person)); personSpecialties.flush();
        selected.forEach(s->personSpecialties.save(new PersonSpecialty(person,s)));
        Business b=input.business();
        if(b==null) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM professional_credentials WHERE tradesperson_id=? AND scope='BUSINESS'",Integer.class,id)>0) throw new IllegalArgumentException("Remove business credentials before removing the business");
            jdbc.update("DELETE FROM professional_businesses WHERE tradesperson_id=?",id);
        } else {
            if(!b.website().isBlank() && !b.website().matches("https?://[^\\s]+")) throw new IllegalArgumentException("Use an HTTP or HTTPS website");
            String logo = b.logoReference() == null || b.logoReference().isMissingNode()
                    ? existingLogo(id)
                    : reference(b.logoReference());
            mediaReference(id,logo);
            jdbc.update("INSERT INTO professional_businesses(tradesperson_id,name,description,website,phone,address,years_in_business,role,logo_reference) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(tradesperson_id) DO UPDATE SET name=excluded.name,description=excluded.description,website=excluded.website,phone=excluded.phone,address=excluded.address,years_in_business=excluded.years_in_business,role=excluded.role,logo_reference=excluded.logo_reference",id,b.name(),b.description(),b.website(),b.phone(),b.address(),b.yearsInBusiness(),b.role(),logo);
        }
        return details(id);
    }
    record Credential(@NotNull @Pattern(regexp="PERSONAL|BUSINESS") String scope,
            @NotNull @Pattern(regexp="LICENSE|INSURANCE|CERTIFICATION") String type,
            @NotBlank @Size(max=200) String name, @NotNull @Size(max=200) String issuer,
            @NotNull @Size(max=200) String number, @NotNull @Size(max=200) String jurisdiction,
            LocalDate issuedDate, LocalDate expirationDate, @NotNull @Size(max=2000) String notes,
            EvidenceKind evidenceKind, @Size(max=500) String evidenceReference) {
        EvidenceKind kind() { return evidenceKind==null ? EvidenceKind.OTHER : evidenceKind; }
    }
    @PostMapping("/credentials")
    Map<String,Object> addCredential(@PathVariable Long id,@Valid @RequestBody Credential c) {
        owner(id); validateCredential(id,c);
        jdbc.update("INSERT INTO professional_credentials(tradesperson_id,scope,type,name,issuer,number,jurisdiction,issued_date,expiration_date,notes,evidence_kind,evidence_reference) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,c.scope(),c.type(),c.name(),c.issuer(),c.number(),c.jurisdiction(),c.issuedDate(),c.expirationDate(),c.notes(),c.kind().name(),c.evidenceReference());
        return details(id);
    }
    private void validateCredential(Long id,Credential c) {
        String expectedType=c.kind()==EvidenceKind.TRADE_LICENSE ? "LICENSE" : c.kind()==EvidenceKind.INSURANCE ? "INSURANCE" : "CERTIFICATION";
        if(c.kind()!=EvidenceKind.OTHER && !expectedType.equals(c.type())) throw new IllegalArgumentException("Credential category must match its type");
        if(c.scope().equals("BUSINESS") && jdbc.queryForObject("SELECT COUNT(*) FROM professional_businesses WHERE tradesperson_id=?",Integer.class,id)==0) throw new IllegalArgumentException("A business is required");
        if(c.issuedDate()!=null && c.expirationDate()!=null && c.expirationDate().isBefore(c.issuedDate())) throw new IllegalArgumentException("Expiration precedes issue date");
    }
    @PutMapping("/credentials/{credentialId}")
    Map<String,Object> editCredential(@PathVariable Long id,@PathVariable Long credentialId,@Valid @RequestBody Credential c) {
        owner(id); validateCredential(id,c);
        if(jdbc.update("UPDATE professional_credentials SET scope=?,type=?,name=?,issuer=?,number=?,jurisdiction=?,issued_date=?,expiration_date=?,notes=?,evidence_kind=?,evidence_reference=?,verification_status='PROVIDED',confirmed_trade_id=NULL,verified_at=NULL,verification_basis=NULL WHERE id=? AND tradesperson_id=?",c.scope(),c.type(),c.name(),c.issuer(),c.number(),c.jurisdiction(),c.issuedDate(),c.expirationDate(),c.notes(),c.kind().name(),c.evidenceReference(),credentialId,id)!=1) throw new ResourceNotFoundException();
        return details(id);
    }
    @DeleteMapping("/credentials/{credentialId}")
    Map<String,Object> deleteCredential(@PathVariable Long id,@PathVariable Long credentialId) {
        owner(id); if(jdbc.update("DELETE FROM professional_credentials WHERE id=? AND tradesperson_id=?",credentialId,id)!=1) throw new ResourceNotFoundException(); return Map.of("deleted",true);
    }
    record ImageInput(@NotBlank @Size(max=4000000) String base64) {}
    @PostMapping("/media")
    Map<String,String> upload(@PathVariable Long id,@Valid @RequestBody ImageInput input) throws IOException {
        owner(id);
        byte[] bytes=Base64.getDecoder().decode(input.base64());
        // Decode with dimensions checked before allocating the full raster, then re-encode.
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext()) throw new IllegalArgumentException("Use a JPEG or PNG image");
            var reader=readers.next();
            try {
                reader.setInput(stream);
                if(!Set.of("JPEG","PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)) || (long)reader.getWidth(0)*reader.getHeight(0)>16000000) throw new IllegalArgumentException("Use a JPEG or PNG up to 16 megapixels");
                var image=reader.read(0); var output=new ByteArrayOutputStream(); ImageIO.write(image,"png",output);
                String key=UUID.randomUUID().toString();
                jdbc.update("INSERT INTO professional_media(id,tradesperson_id,content_type,content) VALUES(?,?,?,?)",key,id,"image/png",output.toByteArray());
                return Map.of("reference","/api/tradespeople/"+id+"/media/"+key);
            } finally { reader.dispose(); }
        } catch (IOException invalidImage) {
            throw new IllegalArgumentException("The image could not be read", invalidImage);
        }
    }
    @GetMapping("/media/{key}")
    ResponseEntity<byte[]> media(@PathVariable Long id,@PathVariable String key) {
        byte[] data=jdbc.query("SELECT content FROM professional_media WHERE id=? AND tradesperson_id=?",(r,n)->r.getBytes(1),key,id).stream().findFirst().orElseThrow(ResourceNotFoundException::new);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").body(data);
    }
    private void mediaReference(Long id,String reference) {
        if(reference==null || reference.isBlank()) return;
        String prefix="/api/tradespeople/"+id+"/media/";
        if(!reference.startsWith(prefix) || jdbc.queryForObject("SELECT COUNT(*) FROM professional_media WHERE id=? AND tradesperson_id=?",Integer.class,reference.substring(prefix.length()),id)!=1) throw new IllegalArgumentException("Choose an image uploaded by this profile");
    }
    /** Keep omission distinct from an explicit JSON null in record constructor inputs. */
    public static class MediaField extends com.fasterxml.jackson.databind.JsonDeserializer<JsonNode> {
        @Override public JsonNode deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws IOException { return parser.readValueAsTree(); }
        @Override public JsonNode getNullValue(com.fasterxml.jackson.databind.DeserializationContext context) { return com.fasterxml.jackson.databind.node.NullNode.instance; }
        @Override public JsonNode getAbsentValue(com.fasterxml.jackson.databind.DeserializationContext context) { return com.fasterxml.jackson.databind.node.MissingNode.getInstance(); }
    }
    private String existingLogo(Long id) {
        var logos=jdbc.query("SELECT logo_reference FROM professional_businesses WHERE tradesperson_id=?",(r,n)->r.getString(1),id);
        return logos.isEmpty() ? null : logos.get(0);
    }
    private String reference(JsonNode value) {
        if(value==null || value.isNull() || value.isMissingNode()) return null;
        if(!value.isTextual()) throw new IllegalArgumentException("Image reference must be text or null");
        return value.textValue();
    }
    record Photo(@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=MediaField.class) JsonNode reference) {}
    @PutMapping("/photo")
    ApiDtos.ProfileResponse photo(@PathVariable Long id,@RequestBody Photo photo) {
        Tradesperson person=owner(id);
        if(photo.reference()!=null && !photo.reference().isMissingNode()) { String ref=reference(photo.reference()); mediaReference(id,ref); person.setProfileImageReference(ref); }
        return ApiDtos.ProfileResponse.from(people.save(person));
    }
    record Work(@NotBlank @Size(max=200) String title,@NotNull @Size(max=5000) String description,LocalDate completionDate,@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=MediaField.class) JsonNode mediaReference) {}
    @PostMapping("/portfolio")
    ApiDtos.PortfolioResponse addWork(@PathVariable Long id,@Valid @RequestBody Work work) {
        Tradesperson person=owner(id); mediaReference(id,reference(work.mediaReference()));
        PortfolioItem item=portfolio.create(person,work.title(),work.description(),PortfolioProvenance.SELF_REPORTED,null,null,work.completionDate());
        item.setMediaReference(reference(work.mediaReference())); return ApiDtos.PortfolioResponse.from(items.save(item),List.of());
    }
    private PortfolioItem ownedWork(Long id,Long itemId) {
        owner(id); PortfolioItem item=items.findById(itemId).orElseThrow(ResourceNotFoundException::new);
        if(!Objects.equals(item.getTradesperson().getId(),id) || item.isRhpVerified()) throw new SecurityException("Only your external work can be changed"); return item;
    }
    @PutMapping("/portfolio/{itemId}")
    ApiDtos.PortfolioResponse editWork(@PathVariable Long id,@PathVariable Long itemId,@RequestBody Map<String,JsonNode> input) {
        PortfolioItem item=ownedWork(id,itemId);
        // Merge omitted metadata before validation; explicit null still validates or removes an optional field.
        var merged=mapper.createObjectNode();
        merged.put("title",item.getTitle());merged.put("description",item.getDescription());
        if(item.getCompletionDate()!=null) merged.put("completionDate",item.getCompletionDate().toString());
        input.forEach(merged::set);
        Work work=mapper.convertValue(merged,Work.class);
        var errors=validator.validate(work);
        if(!errors.isEmpty()) throw new IllegalArgumentException(errors.iterator().next().getPropertyPath()+" "+errors.iterator().next().getMessage());
        if(work.mediaReference()!=null && !work.mediaReference().isMissingNode()) {
            String ref=reference(work.mediaReference());
            if(!Objects.equals(item.getMediaReference(),ref)) mediaReference(id,ref);
            item.setMediaReference(ref);
        }
        item.updateExternal(work.title(),work.description(),work.completionDate());
        return ApiDtos.PortfolioResponse.from(items.save(item),List.of());
    }
    @DeleteMapping("/portfolio/{itemId}")
    Map<String,Object> deleteWork(@PathVariable Long id,@PathVariable Long itemId) { items.delete(ownedWork(id,itemId)); return Map.of("deleted",true); }
}
