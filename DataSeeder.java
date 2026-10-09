package com.realestate.config;

import com.realestate.entity.*;
import com.realestate.entity.Enums.*;
import com.realestate.repository.*;
import com.realestate.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

/**
 * Inserts demo data on the very first start (only when the users table is empty).
 * Demo images are drawn with Java2D, so no image files need to be shipped.
 *
 * Logins:  admin@realestate.com / Admin@123
 *          ravi.agent@realestate.com / Agent@123   (also priya.agent@..., suresh.agent@...)
 *          arun@customer.com / Customer@123        (also meena@, vignesh@, divya@, ismail@, sanjay@customer.com)
 *          karthik.agent@realestate.com / Agent@123  -> a PENDING agent (approve him from the admin panel)
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final AgentProfileRepository agents;
    private final CategoryRepository categories;
    private final PropertyRepository properties;
    private final FavouriteRepository favourites;
    private final EnquiryRepository enquiries;
    private final SiteVisitRepository visits;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;
    private final FileStorageService files;
    private final boolean enabled;
    private final Random rnd = new Random(42);
    private final Map<String, String> imageCache = new HashMap<>();

    public DataSeeder(UserRepository users, AgentProfileRepository agents, CategoryRepository categories,
                      PropertyRepository properties, FavouriteRepository favourites, EnquiryRepository enquiries,
                      SiteVisitRepository visits, NotificationRepository notifications, PasswordEncoder encoder,
                      FileStorageService files, @Value("${app.seed.enabled:true}") boolean enabled) {
        this.users = users; this.agents = agents; this.categories = categories; this.properties = properties;
        this.favourites = favourites; this.enquiries = enquiries; this.visits = visits; this.notifications = notifications;
        this.encoder = encoder; this.files = files; this.enabled = enabled;
    }

    private static final Color[][] THEMES = {
            {new Color(0x1e3a5f), new Color(0x4f8fc0)}, {new Color(0x2d6a4f), new Color(0x95d5b2)},
            {new Color(0x7b2d26), new Color(0xe5a98d)}, {new Color(0x4a3f6b), new Color(0xb8a9e0)}};

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;
        System.setProperty("java.awt.headless", "true");

        // ---------- categories
        Map<String, Category> cat = new LinkedHashMap<>();
        String[][] cats = {{"Apartment", "Flats in apartment complexes"}, {"Independent House", "Standalone houses"},
                {"Villa", "Luxury villas with garden"}, {"Plot", "Residential plots"},
                {"Commercial Building", "Complete commercial buildings"}, {"Office", "Office spaces"},
                {"Shop", "Retail shops and showrooms"}, {"Land", "Agricultural and investment land"}};
        for (String[] c : cats) {
            Category x = new Category();
            x.setName(c[0]); x.setDescription(c[1]);
            cat.put(c[0], categories.save(x));
        }

        // ---------- users
        User admin = user("System Admin", "admin@realestate.com", "9876500000", "Head Office, Coimbatore", "Admin@123", Role.ADMIN);
        User[] ag = {
                agent("Ravi Kumar", "ravi.agent@realestate.com", "9876500001", "RS Puram, Coimbatore", ApprovalStatus.APPROVED, "Licence TN-RE-1001"),
                agent("Priya Lakshmi", "priya.agent@realestate.com", "9876500002", "Adyar, Chennai", ApprovalStatus.APPROVED, "Licence TN-RE-1002"),
                agent("Suresh Babu", "suresh.agent@realestate.com", "9876500003", "KK Nagar, Madurai", ApprovalStatus.APPROVED, "Licence TN-RE-1003"),
                agent("Karthik Raj", "karthik.agent@realestate.com", "9876500004", "Cumbum, Theni", ApprovalStatus.PENDING, "Aadhaar verification pending")};
        String[][] cu = {{"Arun Prakash", "arun"}, {"Meena Devi", "meena"}, {"Vignesh S", "vignesh"},
                {"Divya Bharathi", "divya"}, {"Mohamed Ismail", "ismail"}, {"Sanjay Kumar", "sanjay"}};
        List<User> customers = new ArrayList<>();
        for (int i = 0; i < cu.length; i++)
            customers.add(user(cu[i][0], cu[i][1] + "@customer.com", "98765" + (10001 + i), "Coimbatore, Tamil Nadu", "Customer@123", Role.CUSTOMER));

        // ---------- properties
        List<Property> list = new ArrayList<>();
        PropertyStatus A = PropertyStatus.AVAILABLE, S = PropertyStatus.SOLD, R = PropertyStatus.RENTED, P = PropertyStatus.PENDING_VERIFICATION, I = PropertyStatus.INACTIVE;
        Purpose SALE = Purpose.SALE, RENT = Purpose.RENT;
        Furnishing UF = Furnishing.UNFURNISHED, SF = Furnishing.SEMI_FURNISHED, FF = Furnishing.FULLY_FURNISHED;
        list.add(prop(ag[0], cat.get("Apartment"), "Residential", "3 BHK Luxury Apartment in RS Puram", "RS Puram", "Coimbatore", SALE, 8500000, 1650, 3, 3, 3, true, SF, A));
        list.add(prop(ag[0], cat.get("Apartment"), "Residential", "2 BHK Apartment near Brookefields Mall", "Saibaba Colony", "Coimbatore", SALE, 5200000, 1100, 2, 2, 2, true, UF, A));
        list.add(prop(ag[0], cat.get("Apartment"), "Residential", "Fully Furnished 2 BHK for Rent", "Peelamedu", "Coimbatore", RENT, 22000, 1050, 2, 2, 2, true, FF, A));
        list.add(prop(ag[0], cat.get("Independent House"), "Residential", "4 BHK Independent House with Garden", "Singanallur", "Coimbatore", SALE, 11500000, 2400, 4, 4, 4, true, SF, A));
        list.add(prop(ag[0], cat.get("Villa"), "Residential", "Premium Gated Community Villa", "Saravanampatti", "Coimbatore", SALE, 18500000, 3200, 4, 4, 5, true, FF, A));
        list.add(prop(ag[0], cat.get("Shop"), "Commercial", "Prime Shop on Main Road", "Gandhipuram", "Coimbatore", RENT, 45000, 600, 0, 0, 1, false, UF, A));
        list.add(prop(ag[0], cat.get("Office"), "Commercial", "Furnished Office Space", "Race Course", "Coimbatore", RENT, 65000, 1500, 0, 0, 2, true, FF, R));
        list.add(prop(ag[0], cat.get("Plot"), "Land", "DTCP Approved Residential Plot", "Vadavalli", "Coimbatore", SALE, 3200000, 2400, 0, 0, 0, false, UF, S));
        list.add(prop(ag[1], cat.get("Apartment"), "Residential", "3 BHK Sea-Facing Apartment", "Adyar", "Chennai", SALE, 14500000, 1800, 3, 3, 3, true, FF, A));
        list.add(prop(ag[1], cat.get("Apartment"), "Residential", "2 BHK Flat for Rent near IT Park", "Velachery", "Chennai", RENT, 28000, 1000, 2, 2, 2, true, SF, A));
        list.add(prop(ag[1], cat.get("Independent House"), "Residential", "Spacious 3 BHK House", "Anna Nagar", "Chennai", SALE, 16800000, 2100, 3, 3, 3, true, SF, A));
        list.add(prop(ag[1], cat.get("Villa"), "Residential", "ECR Beach Villa", "Muttukadu", "Chennai", SALE, 32000000, 3800, 5, 5, 5, true, FF, A));
        list.add(prop(ag[1], cat.get("Commercial Building"), "Commercial", "Three Floor Commercial Building", "T Nagar", "Chennai", SALE, 48000000, 6500, 0, 0, 6, true, UF, A));
        list.add(prop(ag[1], cat.get("Office"), "Commercial", "Co-working Office Floor", "Guindy", "Chennai", RENT, 95000, 2200, 0, 0, 3, true, FF, A));
        list.add(prop(ag[1], cat.get("Apartment"), "Residential", "1 BHK Studio Apartment", "Adyar", "Chennai", RENT, 15000, 550, 1, 1, 1, false, FF, R));
        list.add(prop(ag[1], cat.get("Shop"), "Commercial", "Showroom in Mall Road", "Anna Nagar", "Chennai", SALE, 9500000, 900, 0, 0, 1, true, UF, S));
        list.add(prop(ag[2], cat.get("Independent House"), "Residential", "3 BHK House near Meenakshi Temple", "KK Nagar", "Madurai", SALE, 7800000, 1900, 3, 3, 3, true, SF, A));
        list.add(prop(ag[2], cat.get("Apartment"), "Residential", "2 BHK Apartment for Rent", "KK Nagar", "Madurai", RENT, 14000, 950, 2, 2, 2, true, SF, A));
        list.add(prop(ag[2], cat.get("Plot"), "Land", "Corner Plot in Developing Layout", "Thirupparankundram", "Madurai", SALE, 2400000, 1800, 0, 0, 0, false, UF, A));
        list.add(prop(ag[2], cat.get("Land"), "Land", "2 Acre Agricultural Land with Well", "Cumbum", "Theni", SALE, 6500000, 87120, 0, 0, 0, false, UF, A));
        list.add(prop(ag[2], cat.get("Land"), "Land", "Coconut Farm Land", "Bodinayakanur", "Theni", SALE, 9800000, 130680, 0, 0, 0, false, UF, A));
        list.add(prop(ag[2], cat.get("Shop"), "Commercial", "Timber Market Shop", "Cumbum", "Theni", RENT, 18000, 800, 0, 0, 1, true, UF, A));
        list.add(prop(ag[2], cat.get("Villa"), "Residential", "Hill View Villa", "Kodaikanal Road", "Madurai", SALE, 12500000, 2600, 4, 4, 4, true, SF, P));
        list.add(prop(ag[2], cat.get("Apartment"), "Residential", "Budget 2 BHK Flat", "Anna Nagar", "Madurai", SALE, 3900000, 880, 2, 2, 2, false, UF, P));
        list.add(prop(ag[0], cat.get("Independent House"), "Residential", "Old House Needs Renovation", "Ukkadam", "Coimbatore", SALE, 4200000, 1500, 3, 3, 2, false, UF, I));

        // ---------- images
        int n = 0;
        for (Property p : list) {
            int theme = n % THEMES.length;
            boolean land = p.getPropertyType().equals("Land");
            List<RoomType> rooms = new ArrayList<>(List.of(RoomType.EXTERIOR));
            if (!land) {
                rooms.add(RoomType.LIVING_ROOM);
                if (p.getBhk() > 0) { rooms.add(RoomType.BEDROOM); rooms.add(RoomType.KITCHEN); }
                rooms.add(RoomType.BATHROOM);
                if (p.getCategory().getName().equals("Apartment") || p.getCategory().getName().equals("Villa")) rooms.add(RoomType.BALCONY);
                if (p.isParking()) rooms.add(RoomType.PARKING);
            }
            for (RoomType rt : rooms) addImage(p, roomImage(theme, rt), rt, false);
            if (!land && p.isVerified() && n % 3 == 0) addImage(p, panoImage(theme), RoomType.LIVING_ROOM, true);
            properties.save(p);
            n++;
        }

        // ---------- enquiries / visits / favourites over the last 6 months
        List<Property> open = list.stream().filter(Property::isVerified).toList();
        List<Property> available = list.stream().filter(p -> p.getStatus() == A).toList();
        String[] questions = {"Is the price negotiable?", "Can I get more details about the amenities?", "Is the property ready to move in?",
                "Are there any additional maintenance charges?", "Is a bank loan available for this property?", "What is the nearest school and hospital?"};
        String[] replies = {"Yes, a small negotiation is possible. Please schedule a visit and we can discuss.", "Please check the description. I can share a brochure on request.",
                "Yes, it is ready for immediate possession.", "Maintenance is included in the listed price for the first year.",
                "Yes, most nationalised banks approve loans for this property.", "Both are within 2 km. Happy to show you around."};
        for (int i = 0; i < 36; i++) {
            Property p = open.get(rnd.nextInt(open.size()));
            Enquiry e = new Enquiry();
            e.setProperty(p);
            e.setCustomer(customers.get(rnd.nextInt(customers.size())));
            e.setMessage(questions[rnd.nextInt(questions.length)]);
            e.setCreatedAt(daysAgo(rnd.nextInt(180)));
            int r = rnd.nextInt(10);
            if (r < 5) { e.setStatus(EnquiryStatus.REPLIED); e.setReply(replies[rnd.nextInt(replies.length)]); e.setRepliedAt(e.getCreatedAt().plusHours(5 + rnd.nextInt(30))); }
            else if (r < 7) { e.setStatus(EnquiryStatus.CLOSED); e.setReply("Thank you for your interest."); e.setRepliedAt(e.getCreatedAt().plusDays(1)); }
            enquiries.save(e);
        }
        for (int i = 0; i < 46; i++) {
            boolean future = i >= 36;
            Property p = future ? available.get(rnd.nextInt(available.size())) : open.get(rnd.nextInt(open.size()));
            SiteVisit v = new SiteVisit();
            v.setProperty(p);
            v.setCustomer(customers.get(rnd.nextInt(customers.size())));
            v.setContactNumber(v.getCustomer().getPhone());
            v.setPreferredTime(LocalTime.of(10 + rnd.nextInt(7), rnd.nextBoolean() ? 0 : 30));
            v.setMessage("I would like to see the property.");
            if (future) {
                v.setCreatedAt(daysAgo(rnd.nextInt(3)));
                v.setPreferredDate(LocalDate.now().plusDays(1 + rnd.nextInt(14)));
                VisitStatus[] st = {VisitStatus.PENDING, VisitStatus.APPROVED, VisitStatus.RESCHEDULED};
                v.setStatus(st[rnd.nextInt(st.length)]);
            } else {
                v.setCreatedAt(daysAgo(10 + rnd.nextInt(170)));
                v.setPreferredDate(v.getCreatedAt().toLocalDate().plusDays(2 + rnd.nextInt(5)));
                int r = rnd.nextInt(10);
                v.setStatus(r < 6 ? VisitStatus.COMPLETED : r < 8 ? VisitStatus.REJECTED : VisitStatus.APPROVED);
            }
            visits.save(v);
        }
        for (User c : customers) {
            Set<Long> used = new HashSet<>();
            for (int i = 0; i < 3 + rnd.nextInt(2); i++) {
                Property p = available.get(rnd.nextInt(available.size()));
                if (!used.add(p.getId())) continue;
                Favourite f = new Favourite();
                f.setCustomer(c); f.setProperty(p);
                favourites.save(f);
            }
        }

        // ---------- starter notifications
        note(admin, "New agent registration: Karthik Raj", "/admin/agents.html");
        list.stream().filter(p -> p.getStatus() == P).forEach(p -> note(admin, "New property awaiting verification: " + p.getTitle(), "/admin/properties.html"));
        for (User a : ag) if (!a.getEmail().startsWith("karthik")) note(a, "Welcome! Your agent account is approved.", "/agent/dashboard.html");
        for (User c : customers) note(c, "Welcome to EstateHub! Start by searching properties.", "/properties.html");
        System.out.println(">>> Demo data inserted. Admin login: admin@realestate.com / Admin@123");
    }

    // ------------------------------------------------------------------ helpers
    private User user(String name, String email, String phone, String address, String pw, Role role) {
        User u = new User();
        u.setFullName(name); u.setEmail(email); u.setPhone(phone); u.setAddress(address);
        u.setPassword(encoder.encode(pw)); u.setRole(role);
        return users.save(u);
    }

    private User agent(String name, String email, String phone, String address, ApprovalStatus st, String doc) {
        User u = user(name, email, phone, address, "Agent@123", Role.AGENT);
        AgentProfile p = new AgentProfile();
        p.setUser(u); p.setApprovalStatus(st); p.setDocumentDetails(doc);
        agents.save(p);
        return u;
    }

    private Property prop(User agent, Category c, String type, String title, String location, String city, Purpose purpose,
                          long price, int area, int bhk, int bed, int bath, boolean parking, Furnishing f, PropertyStatus st) {
        Property p = new Property();
        p.setAgent(agent); p.setCategory(c); p.setPropertyType(type); p.setTitle(title);
        p.setLocation(location); p.setAddress("Near Main Road, " + location + ", " + city + ", Tamil Nadu");
        p.setPurpose(purpose); p.setPrice(BigDecimal.valueOf(price)); p.setAreaSqft(area);
        p.setBhk(bhk); p.setBedrooms(bed); p.setBathrooms(bath); p.setParking(parking); p.setFurnishing(f);
        p.setStatus(st);
        p.setVerified(st == PropertyStatus.AVAILABLE || st == PropertyStatus.SOLD || st == PropertyStatus.RENTED);
        if (st == PropertyStatus.INACTIVE) p.setVerificationNote("Documents are incomplete");
        p.setDescription("Well-maintained " + c.getName().toLowerCase() + " located in " + location + ", " + city + ". "
                + "Close to schools, hospitals, public transport and shopping. Clear title and all approvals available. "
                + "Contact the agent to schedule a visit.");
        p.setCreatedAt(daysAgo(rnd.nextInt(150)));
        return properties.save(p);
    }

    private void addImage(Property p, String file, RoomType rt, boolean pano) {
        if (file == null) return;
        PropertyImage i = new PropertyImage();
        i.setProperty(p); i.setFileName(file); i.setRoomType(rt); i.setPanorama(pano);
        p.getImages().add(i);
    }

    private void note(User u, String msg, String link) {
        Notification n = new Notification();
        n.setUser(u); n.setMessage(msg); n.setLink(link);
        notifications.save(n);
    }

    private LocalDateTime daysAgo(int d) {
        return LocalDateTime.now().minusDays(d).withHour(9 + rnd.nextInt(9)).withMinute(rnd.nextInt(60)).withSecond(0).withNano(0);
    }

    // ------------------------------------------------------------------ generated demo pictures
    private String roomImage(int theme, RoomType rt) {
        String key = "seed-" + theme + "-" + rt.name().toLowerCase();
        if (imageCache.containsKey(key)) return imageCache.get(key);
        String rel = draw(key, 1200, 800, THEMES[theme], rt.name().replace('_', ' '), false);
        imageCache.put(key, rel);
        return rel;
    }

    private String panoImage(int theme) {
        String key = "seed-pano-" + theme;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        String rel = draw(key, 2048, 1024, THEMES[theme], "360 LIVING ROOM", true);
        imageCache.put(key, rel);
        return rel;
    }

    private String draw(String key, int w, int h, Color[] colors, String label, boolean pano) {
        String rel = "properties/" + key + ".png";
        Path out = files.root().resolve(rel);
        try {
            if (Files.exists(out)) return rel;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, colors[0], w, h, colors[1]));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(255, 255, 255, 45));
            g.fillRect(0, (int) (h * 0.62), w, h);                                  // floor
            g.setColor(new Color(255, 255, 255, 70));
            if (pano) { for (int x = 0; x < w; x += w / 16) g.fillRect(x, 0, 4, h); }  // vertical guide lines for panning
            else { g.fillRoundRect(w / 8, h / 8, w / 4, h / 3, 20, 20); g.fillRoundRect(w / 2, h / 2, w / 3, h / 6, 30, 30); }
            g.setColor(Color.WHITE);
            try {
                g.setFont(new Font("SansSerif", Font.BOLD, pano ? 90 : 72));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, (w - fm.stringWidth(label)) / 2, h / 2);
                g.setFont(new Font("SansSerif", Font.PLAIN, 32));
                String sub = "EstateHub demo image";
                g.drawString(sub, (w - g.getFontMetrics().stringWidth(sub)) / 2, h / 2 + 60);
            } catch (Throwable ignored) { /* fonts missing on server: keep the gradient only */ }
            g.dispose();
            ImageIO.write(img, "png", out.toFile());
            return rel;
        } catch (Throwable t) {
            System.err.println("Could not create demo image " + key + ": " + t.getMessage());
            return null;
        }
    }
}
