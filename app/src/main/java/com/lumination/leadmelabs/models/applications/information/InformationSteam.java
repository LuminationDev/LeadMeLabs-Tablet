package com.lumination.leadmelabs.models.applications.information;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * A class to hold information related to Steam applications
 */
public class InformationSteam {
    private static final HashMap<String, Information> dataMap = new HashMap<>();

    public static HashMap<String, Information> getDataMap() {
        return dataMap;
    }

    //Add each of the experience's details below
    static {
        // Google Earth VR
        Information googleEarth = new Information(
                "Explore the world from totally new perspectives in virtual reality. Stroll the streets of Tokyo, soar over the Grand Canyon, or walk around the Eiffel Tower.",
                new ArrayList<>(Arrays.asList(TagConstants.HASS, TagConstants.MATHS, TagConstants.SCIENCE)),
                new ArrayList<>(Collections.singletonList(TagConstants.GEOGRAPHY)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("348250", googleEarth);

        // Tilt Brush
        Information tiltBrush = new Information(
                "Unleash your creativity with three-dimensional brush strokes, stars, light, and even fire. Your room is your canvas. Your palette is your imagination. The possibilities are endless.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.PAINTING, TagConstants.DIGITAL_ART)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("327140", tiltBrush);

        // All-In-One Sports VR
        Information allInOneSportsVr = new Information(
                "There are 11 sports that students can practice in VR using this app.",
                new ArrayList<>(Collections.singletonList(TagConstants.HEALTH_PE)),
                new ArrayList<>(Arrays.asList(TagConstants.SPORTS, TagConstants.GAMES)),
                new ArrayList<>(Arrays.asList(9, 10, 11, 12, 13, 14, 15))
        );
        dataMap.put("1514840", allInOneSportsVr);

        // Nefertari
        Information nefertari = new Information(
                "Step inside Nefertari's tomb and immerse yourself in the story of its art, history, construction, and mythology through interactive elements.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.ANCIENT_EGYPT)),
                new ArrayList<>(Arrays.asList(13, 14, 15, 16, 17))
        );
        dataMap.put("861400", nefertari);

        // Curious Alice
        Information curiousAlice = new Information(
                "An immersive, interactive experience of the legendary tale, ‘Alice in Wonderland’. Students can follow their own personal White Rabbit companion, hunt for missing objects, solve the Caterpillar's mind-bending riddles, visit the Queen of Hearts’ croquet garden, and experience other classic moments.",
                new ArrayList<>(Collections.singletonList(TagConstants.ENGLISH)),
                new ArrayList<>(Arrays.asList(TagConstants.LITERATURE, TagConstants.BOOK_STUDIES)),
                new ArrayList<>(Arrays.asList(14, 15, 16, 17))
        );
        dataMap.put("1424190", curiousAlice);

        // Trash Time
        Information trashTime = new Information(
                "Practise recycling concepts and use creative and problem solving skills by analysing what needs to be done to meet the objectives of the game (earn money for upgrades by recycling correctly, spend money on upgrades to improve output, tracking CO2 levels, spatial awareness, etc)",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.HASS)),
                new ArrayList<>(Collections.singletonList(TagConstants.ENVIRONMENT)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1029110", trashTime);

        // Space Dance Harmony
        Information spaceDanceHarmony = new Information(
                "A VR dance rhythm game where you replicate real dance choreographies with your entire body to play.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Collections.singletonList(TagConstants.DANCE)),
                new ArrayList<>(Arrays.asList(12, 13))
        );
        dataMap.put("1494670", spaceDanceHarmony);

        // Blocks by Google
        Information blocks = new Information(
                "Build prototypes of 3D objects in poly format using an understanding of shapes, colours, and scale/measurement. Also need to practise spatial awareness / 'thinking in 3D'.",
                new ArrayList<>(Arrays.asList(TagConstants.DESIGN_TECH, TagConstants.MATHS)),
                new ArrayList<>(Collections.singletonList(TagConstants.BUSINESS)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("533970", blocks);

        // Earthquake Simulator
        Information earthquakeSimulator = new Information(
                "Empathise with what is like to prepare for an emergency situation and experiene what it is like to be in an earthquake.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.SAFETY, TagConstants.FIRST_AID)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14))
        );
        dataMap.put("607590", earthquakeSimulator);

        // The Book Of Distance
        Information bookOfDistance = new Information(
                "In 1935, Yonezo Okita left his home in Japan and began a new life in Canada. Then war and state-sanctioned racism changed everything—he became the enemy. His grandson leads us on an interactive virtual pilgrimage through an emotional geography of immigration and family to recover what was lost.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.CULTURAL_STUDIES)),
                new ArrayList<>(Arrays.asList(12, 13, 14, 15))
        );
        dataMap.put("1245640", bookOfDistance);

        // Skytropolis
        Information skytropolis = new Information(
                "Experience what it's like to build a vertical city that puts sustainable practices into consideration. Students are also able to simulate what it's like to calculate costs of building a thriving smart city.",
                new ArrayList<>(Arrays.asList(TagConstants.HASS, TagConstants.MATHS)),
                new ArrayList<>(Arrays.asList(TagConstants.ENVIRONMENT, TagConstants.BUSINESS)),
                new ArrayList<>(Arrays.asList(12, 13, 14, 15, 16, 17))
        );
        dataMap.put("629040", skytropolis);

        // Tiny Town VR
        Information tinyTownVr = new Information(
                "Create an original world and fill it with buildings, roads, vehicles and more. Give it life with posable characters. Tell a story with custom speech bubbles and in-game photo captures. Then, share with friends!",
                new ArrayList<>(Arrays.asList(TagConstants.ENVIRONMENT, TagConstants.DESIGN_TECH)),
                new ArrayList<>(Collections.singletonList(TagConstants.WORLD_BUILDING)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14))
        );
        dataMap.put("653930", tinyTownVr);

        // Mondly
        Information mondly = new Information(
                "Build your fluency in 30 languages: English, Spanish, German, French, Italian, Arabic, Russian, Korean, Japanese, Chinese & more.",
                new ArrayList<>(Collections.singletonList(TagConstants.LANGUAGES)),
                new ArrayList<>(Arrays.asList(TagConstants.CONVERSATIONAL, TagConstants.VOCABULARY)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14))
        );
        dataMap.put("1141930", mondly);

        // Gadgeteer
        Information gadgeteer = new Information(
                "A physics-based IMVR puzzle game where students can build chain reaction machines to solve fun, intricate puzzles. The machines will use gadgets to launch, bump, twist, and turn—creating chain reactions. This app may be used as an alternative to Thingamajig.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.ENGINEERING, TagConstants.PUZZLES)),
                new ArrayList<>(Arrays.asList(13, 14, 15, 16, 17))
        );
        dataMap.put("746560", gadgeteer);

        // Curatours
        Information curatours = new Information(
                "Museum of Plastic 2121 is the first museum tour available on Curatours! Built 100 years from now in an imagined future where things have worked out well for the planet and for humanity. Visitors uncover the story of plastic - its history, science, industry, and impact on our environment.",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.HISTORY)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.MUSEUM)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14))
        );
        dataMap.put("1532110", curatours);

        // GeoGebra
        Information geoGebra = new Information(
                "Place math objects in the VR world, walk around them, and take screenshots from different angles. Millions of people around the world use GeoGebra to learn mathematics and science. This app includes several examples of 3D math objects that you can place in the VR world. Guided activities lead you to discover math in the real world by taking screenshots from different perspectives.",
                new ArrayList<>(Collections.singletonList(TagConstants.MATHS)),
                new ArrayList<>(Arrays.asList(TagConstants.GEOMETRY, TagConstants.PERSPECTIVE)),
                new ArrayList<>(Arrays.asList(14, 15, 16, 17))
        );
        dataMap.put("880270", geoGebra);

        // Sculptr VR
        Information sculptrVr = new Information(
                "Unleash your creativity by building with SculptrVR’s intuitive and fun creation tools.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.SCULPTING, TagConstants.VISUAL_ARTS)),
                new ArrayList<>(Arrays.asList(12, 13, 14))
        );
        dataMap.put("418520", sculptrVr);

        // Mona Lisa: Beyond The Glass
        Information monaLisa = new Information(
                "Visit The Louvre and have a deep dive learning experience of one of the most famous paintings on earth.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.ART_STUDIES, TagConstants.PAINTING)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1172310", monaLisa);

        // Home - A VR Spacewalk
        Information homeSpacewalk = new Information(
                "Simulate the experience of a new astronaut learning the ropes in the International Space Station.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.ASTRONOMY, TagConstants.SPACE_STATION)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("512270", homeSpacewalk);

        // Journey to the Center of the Cell
        Information journeyCell = new Information(
                "This experience takes users on a journey to a microscopic world. Explore the surface and interior of a cancer cell, exploring and learning about cellular anatomy and processes.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.BIOLOGY, TagConstants.HUMAN_ANATOMY)),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("1308470", journeyCell);

        // IL DIVINO: Michelangelo's Sistine Ceiling in VR
        Information sistineCeiling = new Information(
                "Step onto Michelangelo’s scaffold to learn about how he painted the ceiling, or enter a Vatican conservator’s mobile aerial platform to see the ceiling up close and learn about the controversial cleaning. Over 100 clickable elements about Michelangelo’s work.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.ART_STUDIES, TagConstants.MUSEUM)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1165850", sistineCeiling);

        // Harvest Simulator
        Information harvestSimulator = new Information(
                "Simulate the experience of running a farm in the traditional way by learning how to till the soil, plant seeds, care for the plants and watch them grow, then sell them to help the farm.",
                new ArrayList<>(Arrays.asList(TagConstants.HASS, TagConstants.SCIENCE)),
                new ArrayList<>(Collections.singletonList(TagConstants.FARMING)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15))
        );
        dataMap.put("612030", harvestSimulator);

        // 1943 Berlin Blitz
        Information berlinBlitz = new Information(
                "Simulate the experience of joining a Lancaster bomber crew during a raid over Berlin in World War Two.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.WORLD_WAR_TWO)),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("513490", berlinBlitz);

        // Google Spotlight Stories: Pearl
        Information googleSpotlightPearl = new Information(
                "Pearl VR follows a girl and her dad as they crisscross the country chasing their dreams. It’s a story about the gifts we hand down and their power to carry love. And finding grace in the unlikeliest of places. EMMY AWARD in INTERACTIVE STORYTELLING. OSCAR NOMINEE BEST ANIMATED SHORT.",
                new ArrayList<>(Collections.singletonList(TagConstants.ENGLISH)),
                new ArrayList<>(Arrays.asList(TagConstants.INTERACTIVE_STORY, TagConstants.FAMILY)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("476540", googleSpotlightPearl);

        // The VR Museum of Fine Art
        Information museumOfFineArt = new Information(
                "Explore a virtual museum in room-scale VR: see famous sculptures in full, 1:1 scale and see famous paintings without the limitations of glass and security guards.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.ART_STUDIES, TagConstants.MUSEUM)),
                new ArrayList<>(Arrays.asList(13, 14, 15, 16, 17))
        );
        dataMap.put("515020", museumOfFineArt);

        // The Night Cafe
        Information nightCafe = new Information(
                "A VR environment that allows you to explore the world of Vincent van Gogh. Explore some of Van Gogh's pieces in 3 dimensions , with the vivid colors straight from his palette.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.ART_STUDIES, TagConstants.VINCENT_VAN_GOGH)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14))
        );
        dataMap.put("482390", nightCafe);

        // Amazon Odyssey
        Information amazonOdyssey = new Information(
                "Experience the Amazon rainforest with this quest-driven activity where students build a fire, take photos of animals and environment while they learn about each one and explore surroundings.",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.HASS)),
                new ArrayList<>(Collections.singletonList(TagConstants.ENVIRONMENT)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("570540", amazonOdyssey);

        // Neotrie VR
        Information neotrieVr = new Information(
                "Practise student knowledge and skills in geometry and measurement in an immersive environment.",
                new ArrayList<>(Collections.singletonList(TagConstants.MATHS)),
                new ArrayList<>(Arrays.asList(TagConstants.GEOMETRY, TagConstants.MEASUREMENTS)),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("878620", neotrieVr);

        // Aussie Sports VR
        Information aussieSports = new Information(
                "Explore Australia's favourite sports and play mini games: AFL, Basketball, Cricket, Football and Rugby.",
                new ArrayList<>(Collections.singletonList(TagConstants.HEALTH_PE)),
                new ArrayList<>(Arrays.asList(TagConstants.TEAM_SPORTS, TagConstants.AUSTRALIA)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("508250", aussieSports);

        // Dissection Simulator: Frog Edition
        Information dissectionFrog = new Information(
                "Join Wendy Martin, a national award-winning science teacher, to make Ribbit-ing Discoveries in this virtual reality Frog Dissection. Learn the fundamentals of the female frog anatomy in a fully immersive virtual reality experience.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.BIOLOGY, TagConstants.ANIMALS)),
                new ArrayList<>(Arrays.asList(14, 15, 16, 17))
        );
        dataMap.put("1046910", dissectionFrog);

        // Arkio
        Information arkio = new Information(
                "Collaboratively sketch urban plans, buildings and interiors like never before using VR, PCs, phones and tablets. Start fresh or import a 3D model, sketch on top of it then export your design to other 3D design tools, including to Revit as native Revit geometry.",
                new ArrayList<>(Arrays.asList(TagConstants.DESIGN_TECH, TagConstants.MATHS)),
                new ArrayList<>(Collections.singletonList(TagConstants.ARCHITECTURE)),
                new ArrayList<>(Arrays.asList(14, 15, 16, 17))
        );
        dataMap.put("1053760", arkio);

        // The Dawn Of Art
        Information dawnOfArt = new Information(
                "Explore the Chauvet Cave 36,000 years ago, with Daisy Ridley’s voice as your guide. It’s in the Ardèche gorges, in the south of France, that our ancestors drew humanity’s first masterpieces, giving life to their beliefs. Maybe you will also meet the cave bear…",
                new ArrayList<>(Arrays.asList(TagConstants.HASS, TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.CAVE_PAINTINGS)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1236560", dawnOfArt);

        // Ocean Rift
        Information oceanRift = new Information(
                "Experience an aquatic safari park in VR. Explore a vivid underwater world full of life including dolphins, sharks, orcas, turtles, sea snakes, rays, manatees, sea lions, whales, and even dinosaurs!",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.HASS)),
                new ArrayList<>(Collections.singletonList(TagConstants.MARINE_BIOLOGY)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15))
        );
        dataMap.put("422760", oceanRift);

        // Solar System VR
        Information solarSystemVr = new Information(
                "",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.ASTRONOMY, TagConstants.PLANETS)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15))
        );
        dataMap.put("1379970", solarSystemVr);

        // Calcflow
        Information calcflow = new Information(
                "Manipulate vectors with your hands, explore vector addition and cross product. See and feel a double integral of a sinusoidal graph in 3D, a mobius strip and it's normal, and spherical coordinates! Create your own parametrized function and vector field!",
                new ArrayList<>(Collections.singletonList(TagConstants.MATHS)),
                new ArrayList<>(Arrays.asList(TagConstants.STATISTICS, TagConstants.GEOMETRY)),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("547280", calcflow);

        // Short Circuit VR
        Information shortCircuitVr = new Information(
                "Short Circuit VR is an electronics lab simulator in Virtual Reality. You can build your own electronic circuits with the components provided, learn basic electronics by completing challenges and just have fun while experimenting and making awesome projects!",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.DESIGN_TECH)),
                new ArrayList<>(Collections.singletonList(TagConstants.ELECTRONICS)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13))
        );
        dataMap.put("970800", shortCircuitVr);

        // Great Paintings VR
        Information greatPaintings = new Information(
                "Great Paintings from the Great Museums of the World in Virtual Reality. More than 1000 famous paintings to admire and enjoy.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.ART_STUDIES, TagConstants.MUSEUM)),
                new ArrayList<>(Arrays.asList(9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1511090", greatPaintings);

        // Edmersiv
        Information edmersiv = new Information(
                "An educational lab for VR. Interact with several different educational objects, simulations, animations, environments and games.",
                new ArrayList<>(Arrays.asList(TagConstants.SCIENCE, TagConstants.DESIGN_TECH, TagConstants.ARTS, TagConstants.HASS)),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14))
        );
        dataMap.put("542170", edmersiv);

        // Colosseum VR
        Information colosseumVr = new Information(
                "This guided tour will take you through its rise and downfall, bring you face to face with gladiators, and allow you to experience the glory of roman architecture like never before.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.HISTORY, TagConstants.ROMAN_EMPIRE)),
                new ArrayList<>(Arrays.asList(10, 11, 12))
        );
        dataMap.put("1614850", colosseumVr);

        // Materials VR
        Information materialsVr = new Information(
                "Interact with molecular models in Virtual Reality. Add planes to create cross section. See electron density isosurfaces and load your own CHGCAR files.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.CHEMISTRY, TagConstants.MOLECULES)),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("1564310", materialsVr);

        // iB Cricket
        Information ibCricket = new Information(
                "Step into immersive world-class stadiums with thousands cheering for you and our realistic physics makes it intuitive to learn within 10 mins. Hone your skills, compete in global tournaments & become a vSport star.",
                new ArrayList<>(Collections.singletonList(TagConstants.HEALTH_PE)),
                new ArrayList<>(Arrays.asList(TagConstants.SPORTS, TagConstants.CRICKET)),
                new ArrayList<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("957070", ibCricket);

        // Robotics in VR (DEMO ONLY)
        Information roboticsVr = new Information(
                "Gamified solution, curriculum and content for teaching robotics in VR.",
                new ArrayList<>(Arrays.asList(TagConstants.DESIGN_TECH, TagConstants.MATHS, TagConstants.SCIENCE)),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList(15, 16, 17))
        );
        dataMap.put("1774930", roboticsVr);

        // The Body VR: Journey Inside The Cell
        Information insideTheCell = new Information(
                "An educational virtual reality experience that takes the user inside the human body.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.BIOLOGY, TagConstants.HUMAN_ANATOMY)),
                new ArrayList<>(Arrays.asList(13, 14, 15, 16, 17))
        );
        dataMap.put("451980", insideTheCell);

        // Escape Architect VR
        Information escapeArchitect = new Information(
                "An exciting and challenging multi-room escape environment. Each room will have different themes, centered around the same style and lighthearted tone. Successful escapologists can use the room editor to create and share their own ingenious VR puzzles.",
                new ArrayList<>(Collections.singletonList(TagConstants.DESIGN_TECH)),
                new ArrayList<>(Arrays.asList(TagConstants.PUZZLES, TagConstants.PROBLEM_SOLVING)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("812610", escapeArchitect);

        // Brink Traveler
        Information brinkTraveler = new Information(
                "A virtual travel experience that takes you to amazing natural locations in full 3D to feel like you're really there.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.GEOGRAPHY, TagConstants.TRAVEL)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("1462520", brinkTraveler);

        // Nature Treks VR
        Information natureTreksVr = new Information(
                "Explore tropical beaches, underwater oceans and even take to the stars. Discover over 60 different animals. Command the weather, take control of the night or create and shape your own world.",
                new ArrayList<>(Collections.singletonList(TagConstants.HASS)),
                new ArrayList<>(Arrays.asList(TagConstants.GEOGRAPHY, TagConstants.NATURE)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("587580", natureTreksVr);

        // Lyra VR
        Information lyraVr = new Information(
                "Music creation app that offers everyone a fun and unique music-making experience. Make music in 3D and interact with music sequences from entirely new perspectives in virtual reality.",
                new ArrayList<>(Collections.singletonList(TagConstants.ARTS)),
                new ArrayList<>(Arrays.asList(TagConstants.MUSIC, TagConstants.COMPOSITION)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17))
        );
        dataMap.put("572630", lyraVr);

        // Virtual Presenter Pro
        Information virtualPresenter = new Information(
                "Designed to hone your speaking skills by using the game's in-depth analysis to incrementally improve presentations.",
                new ArrayList<>(Collections.singletonList(TagConstants.ENGLISH)),
                new ArrayList<>(Arrays.asList(TagConstants.PUBLIC_SPEAKING, TagConstants.COMMUNICATION)),
                new ArrayList<>(Arrays.asList(11, 12, 13, 14, 15))
        );
        dataMap.put("1282770", virtualPresenter);

        // HoloLab Champions
        Information holoLabChampions = new Information(
                "Grab your safety goggles and set out on a chemical-burning, liquid-stirring, camera-whirring competition in HoloLAB Champions, a virtual reality chemistry game show.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.CHEMISTRY, TagConstants.GAMES)),
                new ArrayList<>(Arrays.asList(13, 14, 15, 16, 17))
        );
        dataMap.put("696760", holoLabChampions);

        // Mars Odyssey
        Information marsOdyssey = new Information(
                "Land on Earth’s sister planet. Walk the surface of Mars in and interact with full-scale, realistic NASA Landers and Rovers. Learn about the Red Planet, its history, and its geography in this interactive experience.",
                new ArrayList<>(Collections.singletonList(TagConstants.SCIENCE)),
                new ArrayList<>(Arrays.asList(TagConstants.PLANETS, TagConstants.ASTRONOMY)),
                new ArrayList<>(Arrays.asList(10, 11, 12, 13, 14, 15))
        );
        dataMap.put("465150", marsOdyssey);
    }
}
