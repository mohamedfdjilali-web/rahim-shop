import { createClient } from "npm:@supabase/supabase-js@2";
import { importPKCS8, SignJWT } from "npm:jose@6";

/*
|--------------------------------------------------------------------------
| CONFIG
|--------------------------------------------------------------------------
*/

const SUPABASE_URL =
    Deno.env.get("SUPABASE_URL")!;

/*
|--------------------------------------------------------------------------
| Supabase Secret Key
|--------------------------------------------------------------------------
|
| Supabase يوفر SUPABASE_SECRET_KEYS تلقائياً.
|
*/

const secretKeys =
    JSON.parse(
        Deno.env.get("SUPABASE_SECRET_KEYS")!
    );

const SUPABASE_SECRET_KEY =
    secretKeys["default"];

/*
|--------------------------------------------------------------------------
| Supabase Admin Client
|--------------------------------------------------------------------------
*/

const supabase =
    createClient(
        SUPABASE_URL,
        SUPABASE_SECRET_KEY
    );

/*
|--------------------------------------------------------------------------
| Firebase Service Account
|--------------------------------------------------------------------------
*/

const FIREBASE_SERVICE_ACCOUNT_JSON =
    Deno.env.get(
        "FIREBASE_SERVICE_ACCOUNT_JSON"
    );

/*
|--------------------------------------------------------------------------
| Notification Secret
|--------------------------------------------------------------------------
*/

const NOTIFICATION_SECRET =
    Deno.env.get(
        "NOTIFICATION_SECRET"
    );

/*
|--------------------------------------------------------------------------
| JSON RESPONSE
|--------------------------------------------------------------------------
*/

function jsonResponse(
    data: unknown,
    status = 200
): Response {

    return new Response(
        JSON.stringify(
            data,
            null,
            2
        ),
        {
            status,
            headers: {
                "Content-Type":
                    "application/json; charset=utf-8"
            }
        }
    );
}

/*
|--------------------------------------------------------------------------
| FIREBASE ACCESS TOKEN
|--------------------------------------------------------------------------
*/

async function getFirebaseAccessToken(): Promise<string> {

    console.log(
        "STEP 4: Starting Firebase OAuth"
    );

    if (!FIREBASE_SERVICE_ACCOUNT_JSON) {

        throw new Error(
            "FIREBASE_SERVICE_ACCOUNT_JSON secret is missing"
        );
    }

    const serviceAccount =
        JSON.parse(
            FIREBASE_SERVICE_ACCOUNT_JSON
        );

    console.log(
        "Firebase project:",
        serviceAccount.project_id
    );

    if (!serviceAccount.client_email) {

        throw new Error(
            "Firebase client_email is missing"
        );
    }

    if (!serviceAccount.private_key) {

        throw new Error(
            "Firebase private_key is missing"
        );
    }

    const tokenUri =
        serviceAccount.token_uri ||
        "https://oauth2.googleapis.com/token";

    const privateKey =
        await importPKCS8(
            serviceAccount.private_key,
            "RS256"
        );

    const now =
        Math.floor(
            Date.now() / 1000
        );

    const assertion =
        await new SignJWT({

            scope:
                "https://www.googleapis.com/auth/firebase.messaging"

        })

            .setProtectedHeader({
                alg: "RS256",
                typ: "JWT"
            })

            .setIssuer(
                serviceAccount.client_email
            )

            .setSubject(
                serviceAccount.client_email
            )

            .setAudience(
                tokenUri
            )

            .setIssuedAt(now)

            .setExpirationTime(
                now + 3600
            )

            .sign(
                privateKey
            );

    console.log(
        "STEP 4A: Firebase JWT created"
    );

    const response =
        await fetch(
            tokenUri,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/x-www-form-urlencoded"
                },

                body:
                    new URLSearchParams({
                        grant_type:
                            "urn:ietf:params:oauth:grant-type:jwt-bearer",

                        assertion:
                            assertion
                    })
            }
        );

    const responseText =
        await response.text();

    console.log(
        "STEP 4B: Firebase OAuth HTTP:",
        response.status
    );

    if (!response.ok) {

        console.error(
            "Firebase OAuth ERROR:",
            responseText
        );

        throw new Error(
            "Firebase OAuth failed: " +
            responseText
        );
    }

    const data =
        JSON.parse(
            responseText
        );

    if (!data.access_token) {

        throw new Error(
            "Firebase access_token not found"
        );
    }

    console.log(
        "STEP 4C: Firebase access token obtained"
    );

    return data.access_token;
}

/*
|--------------------------------------------------------------------------
| SEND FCM
|--------------------------------------------------------------------------
*/

async function sendFCM(
    accessToken: string,
    projectId: string,
    token: string,
    order: Record<string, unknown>
) {

    console.log(
        "STEP 6: Preparing FCM request"
    );

    const orderId =
        order.order_id != null
            ? String(order.order_id)
            : "";

    const orderNumber =
        order.order_number != null
            ? String(order.order_number)
            : "";

    const customerName =
        order.customer_name != null
            ? String(order.customer_name)
            : "";

    const total =
        order.total != null
            ? String(order.total)
            : "";

    let body =
        "لديك طلب جديد في SHOP-DZ";

    if (orderNumber) {

        body +=
            " #" +
            orderNumber;
    }

    if (customerName) {

        body +=
            "\nالعميل: " +
            customerName;
    }

    if (total) {

        body +=
            "\nالمبلغ: " +
            total +
            " دج";
    }

    const url =
        "https://fcm.googleapis.com/v1/projects/" +
        projectId +
        "/messages:send";

    const payload = {

        message: {

            token: token,

            notification: {

                title:
                    "🔔 طلب جديد",

                body:
                    body
            },

            data: {

                type:
                    "new_order",

                order_id:
                    orderId,

                order_number:
                    orderNumber,

                customer_name:
                    customerName,

                total:
                    total
            },

            android: {

                priority:
                    "HIGH",

                notification: {

                    sound:
                        "default"
                }
            }
        }
    };

    console.log(
        "STEP 6A: Sending FCM request"
    );

    const response =
        await fetch(
            url,
            {
                method: "POST",

                headers: {

                    "Authorization":
                        "Bearer " +
                        accessToken,

                    "Content-Type":
                        "application/json"
                },

                body:
                    JSON.stringify(
                        payload
                    )
            }
        );

    const responseText =
        await response.text();

    console.log(
        "STEP 6B: FCM HTTP:",
        response.status
    );

    console.log(
        "STEP 6C: FCM Response:",
        responseText
    );

    return {

        success:
            response.ok,

        status:
            response.status,

        response:
            responseText
    };
}

/*
|--------------------------------------------------------------------------
| MAIN
|--------------------------------------------------------------------------
*/

Deno.serve(
    async (req: Request): Promise<Response> => {

        console.log(
            "================================================"
        );

        console.log(
            "STEP 1: bright-worker started"
        );

        try {

            console.log(
                "HTTP Method:",
                req.method
            );

            /*
            |--------------------------------------------------------------------------
            | POST ONLY
            |--------------------------------------------------------------------------
            */

            if (
                req.method !== "POST"
            ) {

                return jsonResponse(
                    {
                        success: false,
                        message: "POST only"
                    },
                    405
                );
            }

            /*
            |--------------------------------------------------------------------------
            | CHECK NOTIFICATION SECRET
            |--------------------------------------------------------------------------
            */

            console.log(
                "STEP 1A: Checking notification secret"
            );

            if (!NOTIFICATION_SECRET) {

                console.error(
                    "NOTIFICATION_SECRET is missing"
                );

                return jsonResponse(
                    {
                        success: false,
                        message:
                            "NOTIFICATION_SECRET secret is missing"
                    },
                    500
                );
            }

            const requestSecret =
                req.headers.get(
                    "x-notification-secret"
                );

            if (
                !requestSecret ||
                requestSecret !==
                    NOTIFICATION_SECRET
            ) {

                console.error(
                    "ERROR: Invalid notification secret"
                );

                return jsonResponse(
                    {
                        success: false,
                        message:
                            "Unauthorized"
                    },
                    401
                );
            }

            console.log(
                "STEP 1B: Notification secret OK"
            );

            /*
            |--------------------------------------------------------------------------
            | READ JSON
            |--------------------------------------------------------------------------
            */

            const requestBody =
                await req.json();

            console.log(
                "STEP 2: Request body received"
            );

            console.log(
                "Request body:",
                JSON.stringify(
                    requestBody
                )
            );

            const order =
                (
                    requestBody &&
                    typeof requestBody === "object" &&
                    "order" in requestBody &&
                    requestBody.order
                )
                    ? requestBody.order
                    : requestBody;

            /*
            |--------------------------------------------------------------------------
            | VALIDATE ORDER
            |--------------------------------------------------------------------------
            */

            if (
                !order ||
                typeof order !== "object"
            ) {

                return jsonResponse(
                    {
                        success: false,
                        message:
                            "Invalid order data"
                    },
                    400
                );
            }

            const orderData =
                order as Record<string, unknown>;

            if (
                !orderData.order_id &&
                !orderData.order_number
            ) {

                console.error(
                    "Missing order_id/order_number"
                );

                return jsonResponse(
                    {
                        success: false,
                        message:
                            "order_id or order_number is required"
                    },
                    400
                );
            }

            console.log(
                "Order ID:",
                orderData.order_id ?? "none"
            );

            console.log(
                "Order Number:",
                orderData.order_number ?? "none"
            );

            /*
            |--------------------------------------------------------------------------
            | READ PUSH TOKENS
            |--------------------------------------------------------------------------
            */

            console.log(
                "STEP 3: Reading push_tokens"
            );

            const {
                data: tokens,
                error: tokenError
            } =
                await supabase
                    .from("push_tokens")
                    .select(
                        "id, token"
                    )
                    .eq(
                        "is_active",
                        true
                    );

            if (tokenError) {

                console.error(
                    "push_tokens ERROR:",
                    tokenError
                );

                throw new Error(
                    "push_tokens query failed: " +
                    tokenError.message
                );
            }

            console.log(
                "Token count:",
                tokens?.length ?? 0
            );

            /*
            |--------------------------------------------------------------------------
            | NO TOKENS
            |--------------------------------------------------------------------------
            */

            if (
                !tokens ||
                tokens.length === 0
            ) {

                return jsonResponse({

                    success: false,

                    message:
                        "No active FCM tokens found",

                    sent: 0,

                    failed: 0
                });
            }

            /*
            |--------------------------------------------------------------------------
            | FIREBASE TOKEN
            |--------------------------------------------------------------------------
            */

            const firebaseAccessToken =
                await getFirebaseAccessToken();

            const serviceAccount =
                JSON.parse(
                    FIREBASE_SERVICE_ACCOUNT_JSON!
                );

            const projectId =
                serviceAccount.project_id;

            if (!projectId) {

                throw new Error(
                    "Firebase project_id missing"
                );
            }

            console.log(
                "STEP 5: Firebase project:",
                projectId
            );

            /*
            |--------------------------------------------------------------------------
            | SEND TO ALL ACTIVE TOKENS
            |--------------------------------------------------------------------------
            */

            let sent = 0;

            let failed = 0;

            const results = [];

            for (
                const row of tokens
            ) {

                console.log(
                    "Sending notification to token ID:",
                    row.id
                );

                try {

                    const result =
                        await sendFCM(
                            firebaseAccessToken,
                            projectId,
                            row.token,
                            orderData
                        );

                    if (
                        result.success
                    ) {

                        sent++;

                    } else {

                        failed++;

                        /*
                        |--------------------------------------------------------------------------
                        | DISABLE INVALID TOKEN
                        |--------------------------------------------------------------------------
                        */

                        if (
                            result.status === 400 ||
                            result.status === 404
                        ) {

                            console.log(
                                "Disabling invalid token:",
                                row.id
                            );

                            await supabase
                                .from("push_tokens")
                                .update({
                                    is_active: false
                                })
                                .eq(
                                    "id",
                                    row.id
                                );
                        }
                    }

                    results.push({

                        id:
                            row.id,

                        success:
                            result.success,

                        status:
                            result.status,

                        response:
                            result.response
                    });

                } catch (error) {

                    failed++;

                    console.error(
                        "FCM token ERROR:",
                        row.id,
                        error
                    );

                    results.push({

                        id:
                            row.id,

                        success:
                            false,

                        error:
                            error instanceof Error
                                ? error.message
                                : String(error)
                    });
                }
            }

            /*
            |--------------------------------------------------------------------------
            | FINAL RESPONSE
            |--------------------------------------------------------------------------
            */

            console.log(
                "STEP 7: Notification process completed"
            );

            console.log(
                "Sent:",
                sent
            );

            console.log(
                "Failed:",
                failed
            );

            return jsonResponse({

                success:
                    sent > 0,

                message:
                    sent > 0
                        ? "Notification sent successfully"
                        : "Notification failed",

                order_id:
                    orderData.order_id ?? null,

                order_number:
                    orderData.order_number ?? null,

                token_count:
                    tokens.length,

                sent:
                    sent,

                failed:
                    failed,

                results:
                    results
            });

        } catch (error) {

            console.error(
                "================================================"
            );

            console.error(
                "FATAL FUNCTION ERROR:",
                error
            );

            console.error(
                "================================================"
            );

            return jsonResponse(
                {
                    success: false,

                    message:
                        error instanceof Error
                            ? error.message
                            : String(error)
                },
                500
            );
        }
    }
);
