#no-java

Java implementation of the no- system.

## Quick overview

no- is a system flow designed to keep data off the server whenever possible, allowing for an extremely lightweight, 
portable database while also preventing data access whenever the user if offline. The use case is for highly sensitive 
applications which rely on the protection of data rather than ease of access.

## Core tenets

1. Assuming discretion on the part of the user, it should be impossible to prove their account even exists while they are offline, even in the event of a compromised server.
2. Assuming discretion, it should be impossible to read any interactions between users when they are offline.
3. Users should be capable of permanently destroying their account at any time, even without internet access.
4. Assuming the secret key remains hidden, the database should be portable and sharable.

### How these are achieved

User data is serialized into a file, encrypted based on a user password and a server secret key. This data is then given to users 
rather than stored online, requiring the same user password to decrypt. For validation, the server stores a SHA-512 hash of the user object,
condensing a user object of any practical size (to a limit of 2^128 - 1 bits) into 64 bytes.

To log in, users upload their encrypted user file and provide their password. Influences are applied to the model upon successful validation,
allowing for user-to-user or server-to-user interactions. To log off after changes, a download-upload confirmation process is required to ensure
that the user has a copy of their user file before the old hash is removed and the new one added, at which time user-to-user and user-to-server actions
are executed.

## Potential drawbacks

- Unforgiving: the no- system does not allow for account recovery in the event of a user losing their user file or forgetting their password
- Volatile or memory-hoarding: the no- system allows for messages to be sent to an address that does not currently exist on the server. It is impossible to tell the difference 
between influences which will never be triggered, and those which have simply not be consumed yet. As such, the server requires either a wide memory range, or a certain degree
of volatility.
- Complex: the life cycle of a user login is a complex one, involving upload, download and a final upload with extra seasoning in between. It is not possible to hot-terminate
a session if data has been changed, as changes can only be saved when the user is confirmed to have a copy of their user file (see above: Unforgiving).
- Storage is dependent on a single key: any event requiring a change to the server secret key is likely to void all data until such a time wherein a passover system is designed.

## Things to consider

- With SHA-512 hashes taking up 64 bytes of storage, it's possible to accomodate a million accounts in just 64MB.
- When considering multiple servers with multiple backup points, it's possible to simply make the hash database public, 
masking transfers to backup servers by crowdsourcing false leads.
- A correctly implemented no- system would still be vulnerable to a stealthy server compromise; that is, a server known to use the no- system is
compromised and changes the layer which implements such, saving data somewhere else when users log in. 
- Even considering the above, a stealth compromised server never gives information about the users who have not logged in.
