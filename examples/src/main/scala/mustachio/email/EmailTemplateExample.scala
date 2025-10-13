package examples.mustachio.email

import dev.alteration.branch.mustachio.{Mustachio, Stache}

/** A practical example showing how to generate HTML emails using Mustachio.
  *
  * This example demonstrates:
  *   - Building a complete HTML email template
  *   - Using partials for email components (header, footer)
  *   - Combining sections for dynamic content (order items)
  *   - Conditional rendering for different email types
  *   - A realistic use case for the mustache templating engine
  *
  * To run this example: sbt "examples/runMain
  * examples.mustachio.email.EmailTemplateExample"
  */
object EmailTemplateExample {

  def main(args: Array[String]): Unit = {

    println("=== Mustachio Email Template Example ===\n")

    // Order confirmation email
    println("=== Order Confirmation Email ===\n")

    val orderEmailTemplate = """
      |<!DOCTYPE html>
      |<html>
      |<head>
      |  <meta charset="UTF-8">
      |  <title>Order Confirmation</title>
      |</head>
      |<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
      |  {{>emailHeader}}
      |
      |  <h1>Order Confirmation</h1>
      |
      |  <p>Hi {{customerName}},</p>
      |
      |  <p>Thank you for your order! Your order #{{orderNumber}} has been confirmed.</p>
      |
      |  <h2>Order Details</h2>
      |  <table style="width: 100%; border-collapse: collapse;">
      |    <thead>
      |      <tr style="background-color: #f0f0f0;">
      |        <th style="padding: 10px; text-align: left; border: 1px solid #ddd;">Item</th>
      |        <th style="padding: 10px; text-align: right; border: 1px solid #ddd;">Qty</th>
      |        <th style="padding: 10px; text-align: right; border: 1px solid #ddd;">Price</th>
      |      </tr>
      |    </thead>
      |    <tbody>
      |      {{#items}}
      |      <tr>
      |        <td style="padding: 10px; border: 1px solid #ddd;">{{name}}</td>
      |        <td style="padding: 10px; text-align: right; border: 1px solid #ddd;">{{quantity}}</td>
      |        <td style="padding: 10px; text-align: right; border: 1px solid #ddd;">${{price}}</td>
      |      </tr>
      |      {{/items}}
      |    </tbody>
      |    <tfoot>
      |      <tr style="font-weight: bold; background-color: #f0f0f0;">
      |        <td colspan="2" style="padding: 10px; text-align: right; border: 1px solid #ddd;">Total:</td>
      |        <td style="padding: 10px; text-align: right; border: 1px solid #ddd;">${{total}}</td>
      |      </tr>
      |    </tfoot>
      |  </table>
      |
      |  <h2>Shipping Information</h2>
      |  <p>
      |    {{shippingAddress.street}}<br>
      |    {{shippingAddress.city}}, {{shippingAddress.state}} {{shippingAddress.zip}}
      |  </p>
      |
      |  <p>Estimated delivery: {{estimatedDelivery}}</p>
      |
      |  {{#trackingNumber}}
      |  <p>Track your order: <a href="{{trackingUrl}}">{{trackingNumber}}</a></p>
      |  {{/trackingNumber}}
      |
      |  {{>emailFooter}}
      |</body>
      |</html>
    """.stripMargin.trim

    val emailPartials = Stache.obj(
      "emailHeader" -> Stache.str("""
        |<div style="background-color: #4CAF50; color: white; padding: 20px; text-align: center;">
        |  <h2>{{companyName}}</h2>
        |</div>
      """.stripMargin.trim),
      "emailFooter" -> Stache.str("""
        |<hr style="margin-top: 30px; border: none; border-top: 1px solid #ddd;">
        |<p style="color: #666; font-size: 12px; text-align: center;">
        |  {{companyName}}<br>
        |  {{supportEmail}}<br>
        |  <a href="{{unsubscribeUrl}}">Unsubscribe</a>
        |</p>
      """.stripMargin.trim)
    )

    val orderContext = Stache.obj(
      "companyName"   -> Stache.str("Acme Store"),
      "customerName"  -> Stache.str("Alice Johnson"),
      "orderNumber"   -> Stache.str("12345"),
      "items" -> Stache.Arr(
        List(
          Stache.obj(
            "name"     -> Stache.str("Wireless Mouse"),
            "quantity" -> Stache.str("2"),
            "price"    -> Stache.str("25.99")
          ),
          Stache.obj(
            "name"     -> Stache.str("USB-C Cable"),
            "quantity" -> Stache.str("1"),
            "price"    -> Stache.str("12.99")
          ),
          Stache.obj(
            "name"     -> Stache.str("Laptop Stand"),
            "quantity" -> Stache.str("1"),
            "price"    -> Stache.str("49.99")
          )
        )
      ),
      "total"             -> Stache.str("114.96"),
      "shippingAddress" -> Stache.obj(
        "street" -> Stache.str("123 Main St"),
        "city"   -> Stache.str("Springfield"),
        "state"  -> Stache.str("IL"),
        "zip"    -> Stache.str("62701")
      ),
      "estimatedDelivery" -> Stache.str("January 30, 2025"),
      "trackingNumber"    -> Stache.str("1Z999AA10123456784"),
      "trackingUrl" -> Stache
        .str("https://example.com/track/1Z999AA10123456784"),
      "supportEmail"    -> Stache.str("support@acmestore.com"),
      "unsubscribeUrl"  -> Stache.str("https://example.com/unsubscribe")
    )

    val orderEmail = Mustachio.render(orderEmailTemplate, orderContext, Some(emailPartials))
    println(orderEmail)
    println("\n")

    // Welcome email (simpler example)
    println("=== Welcome Email ===\n")

    val welcomeTemplate = """
      |<!DOCTYPE html>
      |<html>
      |<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
      |  {{>emailHeader}}
      |
      |  <h1>Welcome to {{companyName}}!</h1>
      |
      |  <p>Hi {{firstName}},</p>
      |
      |  <p>Thank you for joining {{companyName}}. We're excited to have you!</p>
      |
      |  <p>Here are some things you can do to get started:</p>
      |  <ul>
      |    {{#features}}
      |    <li>{{.}}</li>
      |    {{/features}}
      |  </ul>
      |
      |  {{#hasPromoCode}}
      |  <div style="background-color: #fffbcc; border: 2px solid #ffd700; padding: 15px; margin: 20px 0;">
      |    <strong>Special Offer!</strong><br>
      |    Use code <strong>{{promoCode}}</strong> for {{discount}}% off your first purchase!
      |  </div>
      |  {{/hasPromoCode}}
      |
      |  <p>If you have any questions, feel free to reach out to our support team.</p>
      |
      |  {{>emailFooter}}
      |</body>
      |</html>
    """.stripMargin.trim

    val welcomeContext = Stache.obj(
      "companyName" -> Stache.str("Acme Store"),
      "firstName"   -> Stache.str("Bob"),
      "features" -> Stache.Arr(
        List(
          Stache.str("Browse our catalog of products"),
          Stache.str("Create your wish list"),
          Stache.str("Track your orders"),
          Stache.str("Join our rewards program")
        )
      ),
      "hasPromoCode" -> Stache.obj(
        "promoCode" -> Stache.str("WELCOME20"),
        "discount"  -> Stache.str("20")
      ),
      "supportEmail"   -> Stache.str("support@acmestore.com"),
      "unsubscribeUrl" -> Stache.str("https://example.com/unsubscribe")
    )

    val welcomeEmail =
      Mustachio.render(welcomeTemplate, welcomeContext, Some(emailPartials))
    println(welcomeEmail)

    println("\n=== Example Complete ===")
  }

}
