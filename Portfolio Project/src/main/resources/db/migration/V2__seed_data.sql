-- Sample content. Replace with your own via a new migration (V3__...) or the admin API.

INSERT INTO profile (full_name, title, tagline, bio, location, email, github_url, linkedin_url,
                     years_experience, projects_completed, happy_clients)
VALUES ('Alex Morgan',
        'Java Full Stack Developer',
        'I build fast, accessible and resilient web applications from database to pixel.',
        'I am a full stack engineer who loves turning complicated problems into simple, elegant products. My daily toolbox is Java, Spring Boot and PostgreSQL on the back end, paired with modern, accessible front ends.

Over the years I have shipped payment platforms, analytics dashboards and internal tools used by thousands of people. I care about clean architecture, automated testing and secure-by-default systems.

When I am not coding I mentor junior developers, contribute to open source and explore the mountains with a camera.',
        'Bengaluru, India',
        'alex.morgan@example.com',
        'https://github.com/example',
        'https://www.linkedin.com/in/example',
        8, 42, 25);

INSERT INTO skill (name, category, level, display_order) VALUES
('Java',            'Backend',   95, 1),
('Spring Boot',     'Backend',   92, 2),
('REST API Design', 'Backend',   90, 3),
('PostgreSQL',      'Data',      85, 4),
('JPA / Hibernate', 'Data',      88, 5),
('JavaScript',      'Frontend',  86, 6),
('HTML & CSS',      'Frontend',  90, 7),
('Accessibility',   'Frontend',  80, 8),
('Docker',          'DevOps',    78, 9),
('CI/CD',           'DevOps',    82, 10),
('Spring Security', 'Security',  84, 11),
('Testing (JUnit)', 'Quality',   88, 12);

INSERT INTO project (title, summary, tech_stack, repo_url, live_url, featured, display_order) VALUES
('PayFlow Gateway',
 'A PCI-conscious payment orchestration service handling 2M+ transactions a month with idempotent APIs and full audit trails.',
 'Java,Spring Boot,PostgreSQL,Kafka', 'https://github.com/example/payflow', NULL, TRUE, 1),
('InsightBoard',
 'Real-time analytics dashboard with streaming charts, role based access and exportable reports.',
 'Java,Spring WebFlux,JavaScript,Redis', 'https://github.com/example/insightboard', 'https://example.com/insightboard', TRUE, 2),
('TaskHive',
 'Collaborative task manager with drag and drop boards, notifications and offline-friendly sync.',
 'Java,Spring Boot,MySQL,WebSocket', 'https://github.com/example/taskhive', NULL, FALSE, 3),
('ShopLite',
 'Lightweight e-commerce back end with catalogue search, carts, orders and a transactional outbox.',
 'Java,Spring Data,PostgreSQL,Docker', 'https://github.com/example/shoplite', NULL, FALSE, 4),
('DocuSign Bridge',
 'Integration microservice that signs and archives documents, with retry, circuit breaking and metrics.',
 'Java,Resilience4j,S3,Micrometer', 'https://github.com/example/docubridge', NULL, FALSE, 5),
('Portfolio',
 'This very site: a Spring Boot REST API with a dependency-free, animated and accessible front end.',
 'Java,Spring Boot,HTML,CSS,JavaScript', 'https://github.com/example/portfolio', NULL, FALSE, 6);

INSERT INTO testimonial (author, role, quote, display_order) VALUES
('Priya Sharma', 'CTO, FinEdge',
 'Alex delivered our payments platform ahead of schedule with outstanding quality. The architecture has scaled effortlessly.', 1),
('Daniel Weber', 'Product Lead, Northwind',
 'A rare engineer who understands both the code and the customer. Communication was clear at every step.', 2),
('Sofia Alvarez', 'Engineering Manager, Cloudly',
 'Mentored our juniors, improved our test coverage from 40% to 90% and never missed a deadline.', 3);
