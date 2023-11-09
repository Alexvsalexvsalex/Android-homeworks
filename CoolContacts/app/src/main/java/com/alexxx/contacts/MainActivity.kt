package com.alexxx.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contact_item.view.*


class MainActivity : AppCompatActivity() {
    val myRequestId = 1703;
    var contactList = ArrayList<Contact>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewManager = LinearLayoutManager(this)
        recyclerView.apply {
            layoutManager = viewManager
            adapter = ContactAdapter(contactList) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:" + it.phoneNumber)
                startActivity(intent)
            }
        }

        checkPermission()

        checkPermissionButton.setOnClickListener { checkPermission() }
    }

    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                myRequestId
            )
        } else {
            processReceivedPermission()
        }
    }

    fun processReceivedPermission() {
        checkPermissionButton.visibility = View.GONE
        checkPermissionTextView.visibility = View.GONE

        contactList.clear()
        contactList.addAll(fetchAllContacts())
        recyclerView.adapter?.notifyDataSetChanged()

        Toast.makeText(
            applicationContext, resources.getQuantityString(
                R.plurals.contacts_loaded,
                contactList.size,
                contactList.size
            ), Toast.LENGTH_LONG
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            myRequestId -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    processReceivedPermission()
                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string.permission_not_received,
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    data class Contact(val name: String, val phoneNumber: String)

    class ContactViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        fun bind(contact: Contact) {
            with(root) {
                personName.text = contact.name
                phoneNumber.text = contact.phoneNumber
            }
        }
    }

    class ContactAdapter(
        private val contacts: List<Contact>,
        private val onClick: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
            val holder = ContactViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.contact_item, parent, false)
            )
            holder.root.setOnClickListener {
                onClick(contacts[holder.adapterPosition])
            }
            return holder
        }

        override fun onBindViewHolder(holder: ContactViewHolder, position: Int) =
            holder.bind(contacts[position])

        override fun getItemCount() = contacts.size
    }

    fun Context.fetchAllContacts(): List<Contact> {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
            .use { cursor ->
                if (cursor == null) return emptyList()
                val builder = ArrayList<Contact>()
                while (cursor.moveToNext()) {
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                            ?: "N/A"
                    val phoneNumber =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            ?: "N/A"

                    builder.add(Contact(name, phoneNumber))
                }
                return builder
            }
    }
}