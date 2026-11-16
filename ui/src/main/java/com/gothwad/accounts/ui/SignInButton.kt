package com.gothwad.accounts.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Standard Material 3 sign-in button inheriting the caller's MaterialTheme. */
@Composable
fun SignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.gothwad_sign_in),
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}
