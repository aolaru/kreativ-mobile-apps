# PaperBuild Pro release setup

## Play Console product

Create a one-time, non-consumable product with these exact values:

- Product ID: `paperbuild_pro_lifetime`
- Name: `PaperBuild Pro`
- EUR base price: €2.99, with automatic Google Play country pricing. Show the returned local price in the app.
- Description: Permanent unlock for smaller PDF exports, unlimited pages, merge, and split.

Do not enable multiple quantity or consume the purchase. This product is a permanent entitlement.

## Test before production

1. Upload the signed bundle to Internal Testing.
2. Add at least one Google account as a license tester.
3. Install through Google Play (not adb) with the tester account.
4. Complete a test purchase, close and reopen the app, then use Restore purchase.
5. Confirm a pending purchase remains locked until Google Play reports it as purchased.
6. Confirm the app unlocks compact export, removes the 10-page free limit, and enables merge and split.
7. Interrupt connectivity during acknowledgement, reopen the app, and confirm the purchase is acknowledged and retained.
8. Check the price displayed by the app matches the selected Google Play purchase option in the tester's country.

## Important

The app acknowledges a completed non-consumable purchase and restores it through Google Play. Before a high-volume launch, add server-side purchase-token verification for stronger fraud protection.

## Google Play reviewer access

Version code 3 adds **Review access** on the Pro screen. A reusable signed code unlocks all three Pro features locally without a purchase or account. **Leave review access** removes only the review grant; Google Play ownership is unchanged. Restore checks do not revoke a valid review grant.

The app includes an EC P-256 public key and verifies a SHA-256/ECDSA signature scoped to PaperBuild Pro. The private key and issued code must remain outside the repository and must not appear in screenshots, logs, test fixtures, or store listing copy. The code is a bearer entitlement: anyone with a copy can use it, so disclose it only in Play Console's reviewer instructions. It is reusable and does not expire; rotating the verification key in a future build revokes old review grants in that build.

The protected local key and code are in `~/Library/Application Support/Kreativ/PaperBuild/reviewer-access/`. Back up the private key securely. Reissue with the same key using `sh scripts/issue-review-code.sh /absolute/private.pem /absolute/new-code.txt` and a modern OpenSSL installation. The script refuses to overwrite an existing code file or use a key that doesn't match the app.

Reviewer instructions: Open PaperBuild → Explore Pro → scroll to Review access → paste the privately supplied code → Activate → Back to my documents. No login, payment, trial, or network connection is needed for this grant. Do not mark App access as unrestricted: the normal Pro features still require payment or a valid review grant.

For the real-key device regression, supply `-Pandroid.testInstrumentationRunnerArguments.reviewCode` privately to `connectedQaAndroidTest`. Without it, the real-key test is skipped; the remaining rejection/purchase-isolation and JVM signature tests still run. Never check in a real code as a test fixture.
