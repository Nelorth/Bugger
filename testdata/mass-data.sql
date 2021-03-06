/*
 * Inserts mass data into the database.
 *
 * This SQL script will fill the entity and relationship tables with
 * automatically generated mass data. This will give a feel for how the system
 * works with populated tables.
 */


INSERT INTO "user" (username, email_address, first_name, last_name,
    password_hash, password_salt, hashing_algorithm, is_admin) VALUES
    (CONCAT('many-subs', CURRVAL('user_id_seq') + 1), CONCAT('many-subs', CURRVAL('user_id_seq') + 1, '@example.com'), 'User With', 'Many Subs', 'cb64f9739595a2eb5d58cb7a291aed0b0627f4efcbbf1a6b1c5e5864df3f6c941a0495fad7939cdd810bc74852a670ca14a9ae5033843c8d233d2a4f33b11393', 'aa35afbed60537ff39a5be70dc1d183fbf6614ea5ce7d36c2e5f154d2d3e1706d9429f8597fb12fd4d0601391aaa5684d15d8d0078645b4946acf5512766fc25', 'SHA3-512', false)
ON CONFLICT DO NOTHING;

INSERT INTO topic (title, description) VALUES
    (CONCAT('Topic with many subs ', CURRVAL('topic_id_seq') + 1), 'This topic has many subscribers.');

INSERT INTO report (topic, title, type, severity, version) VALUES
    (CURRVAL('topic_id_seq'), 'Report with many subs', 'BUG', 'MINOR', '');


-- Insert 50 regular users, 50 moderators, and 50 additional users that will be
-- banned.
DO
$$
BEGIN
FOR i IN 1..50 LOOP
    -- Password: BuggerFahrenMachtSpass42
    INSERT INTO "user" (username, email_address, first_name, last_name,
        password_hash, password_salt, hashing_algorithm, is_admin) VALUES
    (CONCAT('testuser', CURRVAL('user_id_seq') + 1), CONCAT('testuser', CURRVAL('user_id_seq') + 1, '@example.com'), CONCAT('testuser', CURRVAL('user_id_seq')), 'lastname', 'a4fb35872e742d37099c997deff6f0978ab1f27b8ec420231bca0db956adfbd38492994c38f7ef7521fdd76fb39a04616f6c18902dfbc0b2f38a97420133427c', 'bd47d0c5c91557cbbea1beaeb5b11860fa4f9d2577dce262d3ea87c5ad45fa577746d445cd298627aa659075c1cbfe496b5033782275065947fa1d9273a9cd77', 'SHA3-512', false),
    (CONCAT('testmoderator', CURRVAL('user_id_seq') + 1), CONCAT('testmoderator', CURRVAL('user_id_seq') + 1, '@example.com'), CONCAT('testmoderator', CURRVAL('user_id_seq')), 'lastname', 'a4fb35872e742d37099c997deff6f0978ab1f27b8ec420231bca0db956adfbd38492994c38f7ef7521fdd76fb39a04616f6c18902dfbc0b2f38a97420133427c', 'bd47d0c5c91557cbbea1beaeb5b11860fa4f9d2577dce262d3ea87c5ad45fa577746d445cd298627aa659075c1cbfe496b5033782275065947fa1d9273a9cd77', 'SHA3-512', false),
    (CONCAT('testbanneduser', CURRVAL('user_id_seq') + 1), CONCAT('testbanneduser', CURRVAL('user_id_seq') + 1, '@example.com'), CONCAT('testbanneduser', CURRVAL('user_id_seq')), 'lastname', 'a4fb35872e742d37099c997deff6f0978ab1f27b8ec420231bca0db956adfbd38492994c38f7ef7521fdd76fb39a04616f6c18902dfbc0b2f38a97420133427c', 'bd47d0c5c91557cbbea1beaeb5b11860fa4f9d2577dce262d3ea87c5ad45fa577746d445cd298627aa659075c1cbfe496b5033782275065947fa1d9273a9cd77', 'SHA3-512', false);
END LOOP;
END;
$$
;

-- Insert 3 topics, each with 100 reports, each with 100 posts, each with 2
-- empty attachments.
DO
$$
BEGIN
FOR i IN 1..3 LOOP
    INSERT INTO topic (title, description) VALUES
        (CONCAT('testtopic', CURRVAL('topic_id_seq') + 1), CONCAT('Description for testtopic', CURRVAL('topic_id_seq') + 1));

    FOR j IN 1..100 LOOP
        INSERT INTO report (topic, title, type, severity) VALUES
            (CURRVAL('topic_id_seq'), CONCAT('testreport', CURRVAL('report_id_seq') + 1), 'BUG', 'MINOR');

        FOR k IN 1..100 LOOP
            INSERT INTO post (report, content) VALUES
                (CURRVAL('report_id_seq'), CONCAT('testpost', CURRVAL('post_id_seq') + 1));

            INSERT INTO attachment (post, content, name, mimetype) VALUES
                (CURRVAL('post_id_seq'), '', CONCAT('testattachment', 2 * CURRVAL('post_id_seq'), '.txt'), 'text/plain'),
                (CURRVAL('post_id_seq'), '', CONCAT('testattachment', 2 * CURRVAL('post_id_seq') + 1, '.txt'), 'text/plain');
        END LOOP;
    END LOOP;
END LOOP;
END;
$$
;

-- Insert 100 topics, each with 5 reports, each with 5 posts.
DO
$$
BEGIN
FOR i IN 4..100 LOOP
    INSERT INTO topic (title, description) VALUES
        (CONCAT('testtopic', CURRVAL('topic_id_seq') + 1), CONCAT('Description for testtopic', CURRVAL('topic_id_seq') + 1));

    FOR j IN 1..5 LOOP
        INSERT INTO report (topic, title, type, severity, created_by) VALUES
            (CURRVAL('topic_id_seq'), CONCAT('testreport', CURRVAL('report_id_seq') + 1), 'BUG', 'MINOR', CURRVAL('user_id_seq'));

        FOR k IN 1..5 LOOP
            INSERT INTO post (report, content, created_by) VALUES
                (CURRVAL('report_id_seq'), CONCAT('testpost', CURRVAL('post_id_seq') + 1), CURRVAL('user_id_seq'));
        END LOOP;
    END LOOP;
END LOOP;
END;
$$
;

-- Add many subscriptions, moderators, bans, and relevance votes.
INSERT INTO user_subscription (subscriber, subscribee)
SELECT subscriber.id, CURRVAL('user_id_seq')
    FROM "user" AS subscriber
    WHERE subscriber.username LIKE 'testuser%'
ON CONFLICT DO NOTHING;

INSERT INTO user_subscription (subscriber, subscribee)
SELECT CURRVAL('user_id_seq'), subscribee.id
    FROM "user" AS subscribee
    WHERE subscribee.username LIKE 'testuser%'
ON CONFLICT DO NOTHING;

INSERT INTO topic_subscription (subscriber, topic)
SELECT subscriber.id, CURRVAL('topic_id_seq')
    FROM "user" AS subscriber
    WHERE subscriber.username LIKE 'testuser%'
ON CONFLICT DO NOTHING;

INSERT INTO report_subscription (subscriber, report)
SELECT subscriber.id, CURRVAL('report_id_seq')
    FROM "user" AS subscriber
    WHERE subscriber.username LIKE 'testuser%'
ON CONFLICT DO NOTHING;

INSERT INTO topic_moderation (moderator, topic)
SELECT moderator.id, CURRVAL('topic_id_seq')
    FROM "user" AS moderator
    WHERE moderator.username LIKE 'testmoderator%'
ON CONFLICT DO NOTHING;

INSERT INTO topic_ban (outcast, topic)
SELECT outcast.id, CURRVAL('topic_id_seq')
    FROM "user" AS outcast
    WHERE outcast.username LIKE 'testbanneduser%'
ON CONFLICT DO NOTHING;

INSERT INTO relevance_vote (voter, report, weight)
SELECT voter.id, CURRVAL('report_id_seq'), FLOOR(RANDOM() * 10)
    FROM "user" AS voter
    WHERE voter.username LIKE 'testuser%'
ON CONFLICT DO NOTHING;

