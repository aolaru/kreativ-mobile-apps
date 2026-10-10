# PaperBuild Pro release setup

## Play Console product

Create a one-time, non-consumable product with these exact values:

- Product ID: `paperbuild_pro_lifetime`
- Name: `PaperBuild Pro`
- Default price: €2.99
- Description: Permanent unlock for smaller PDF exports, unlimited pages, merge, and split.

Do not enable multiple quantity or consume the purchase. This product is a permanent entitlement.

## Test before production

1. Upload the signed bundle to Internal Testing.
2. Add at least one Google account as a license tester.
3. Install through Google Play (not adb) with the tester account.
4. Complete a test purchase, close and reopen the app, then use Restore purchase.
5. Confirm a pending purchase remains locked until Google Play reports it as purchased.
6. Confirm the app unlocks compact export, removes the 10-page free limit, and enables merge and split.

## Important

The app acknowledges a completed non-consumable purchase and restores it through Google Play. Before a high-volume launch, add server-side purchase-token verification for stronger fraud protection.
